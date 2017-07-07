package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.EventIdListConverter
import com.plexiti.commons.application.CommandStatus.*
import com.plexiti.commons.domain.*
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.io.Serializable
import java.util.*
import javax.persistence.*
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
open class Command(): Message {

    constructor(name: Name): this() {
        this.name = name
    }

    override val type = MessageType.Command

    override var name = Name(name = this::class.simpleName!!)

    open val definition: Int = 0

    override var id = CommandId(UUID.randomUUID().toString())
        protected set

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    var issuedAt = Date()
        protected set

    companion object {

        var store = CommandStore()

        fun <C: Command> issue(command: C): C {
            return store.save(command)
        }

        fun fromJson(json: String): Command {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val name = node.get("name").textValue()
            val type = store.type(Name(name))
            return fromJson(json, type)
        }

        fun <C: Command> fromJson(json: String, type: KClass<C>): C {
            val command =  ObjectMapper().readValue(json, type.java)
            command.construct()
            return command
        }

    }

    open fun internals(): StoredCommand {
        return store.toEntity(this)!!
    }

    open fun construct() {}

    open fun <C: Command> command(type: KClass<out C>): C? {
        if (internals().issuedBy != null) {
            return Command.store.findFirstByName_AndIssuedBy_OrderByIssuedAtDesc(Command.store.names[type]!!, internals().issuedBy!!) as C?
        } else {
            throw IllegalStateException()
        }
    }

    open fun <E: Event> event(type: KClass<out E>): E? {
        if (internals().issuedBy != null) {
            return Event.store.findFirstByName_AndRaisedBy_OrderByRaisedAtDesc(Event.store.names[type]!!, internals().issuedBy!!) as E?
        } else {
            throw IllegalStateException()
        }
    }

    open fun correlation(): Correlation {
        return Correlation.create(id.value)!!
    }

    open fun correlation(event: Event): Correlation? {
        return Correlation.create(event.internals().raisedBy?.value)
    }

    open fun trigger(event: Event): Command? {
        return null
    }

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Command) return false
        if (id != other.id) return false
        return true
    }

}

@Entity
@Table(name="COMMANDS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING, length = 128)
@DiscriminatorValue(MessageType.Discriminator.command)
@NamedQueries(
    NamedQuery(
        name = "CommandForwarder",
        query = "select c from StoredCommand c where c.forwardedAt is null and type(c) = StoredCommand"
    ),
    NamedQuery(
        name = "FlowResultForwarder",
        query = "select c from StoredCommand c where c.execution.finishedAt is not null and c.processedAt is null"
    )
)
open class StoredCommand(): StoredMessage<CommandId, CommandStatus>() {

    constructor(command: Command) : this() {
        this.name = command.name
        this.id = command.id
        this.issuedAt = command.issuedAt
        this.correlatedBy = command.correlation()
        this.json = ObjectMapper().writeValueAsString(command)
        this.status = if (this.name.context == Name.context) issued else forwarded
    }

    @Transient
    override val type = MessageType.Command

    @Column(name = "ISSUED_AT", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    var issuedAt = Date()
        internal set

    @Column(name = "PROCESSED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var processedAt: Date? = null
        internal set

    @Embedded @AttributeOverride(name = "value", column = Column(name = "TRIGGERED_BY_EVENT", nullable = true, length=36))
    var triggeredBy: EventId? = null
        internal set

    @Embedded @AttributeOverride(name = "value", column = Column(name = "ISSUED_BY_COMMAND", nullable = true, length=36))
    open var issuedBy: CommandId? = null
        internal set

    @Embedded @AttributeOverride(name = "key", column = Column(name = "CORRELATED_BY_KEY", length = 128, nullable = false))
    lateinit var correlatedBy: Correlation
        internal set

    @Column(name="CORRELATED_TO_EVENTS", nullable = true, length=4096)
    @Convert(converter = EventIdListConverter::class)
    var correlatedToEvents: List<EventId>? = null
        internal set

    @Embedded @AttributeOverride(name="value", column = Column(name="CORRELATED_TO_TOKEN", nullable = true))
    var correlatedToToken: TokenId? = null
        internal set

    @Embedded @AttributeOverride(name="value", column = Column(name="RESULTING_IN", nullable = true, length = 40))
    var resultingIn: Hash? = null
        internal set

    @Embedded
    var execution: Execution = Execution()
        internal set

    @Embedded
    var problem: Problem? = null
        internal set

    fun forward() {
        this.status = forwarded
        this.forwardedAt = Date()
    }

    fun start() {
        this.status = started
        this.execution = Execution()
        this.execution.startedAt = Date()
    }

    fun process() {
        this.status = processed
        this.processedAt = Date()
    }

    fun correlate(result: Any) {
        if (result is Event) {
            val event = result
            execution.finishedAt = event.raisedAt
            correlatedToEvents = correlatedToEvents?.plus(result.id) ?: listOf(result.id)
        } else if (result is Problem) {
            problem = result
            execution.finishedAt = result.occuredAt
            // TODO remove events from correlatedToEvents which were raised in this transation
        } else if (result is Value) {
            execution.finishedAt = Date()
            resultingIn = Hash(result)
        }
        if (issuedBy != null) {
            status = finished
        } else {
            status = processed
            processedAt = execution.finishedAt
        }
    }

    @Consumed
    fun consumed(){
        when (status) {
            issued -> forward()
            finished -> process()
            else -> IllegalStateException()
        }
    }

}

open class CommandId(value: String = ""): MessageId(value)

@Embeddable
class Correlation : Serializable {

    @Column(name = "KEY", length = 128, nullable = false)
    lateinit var key: String
        @JsonValue get
        @JsonValue private set

    override fun toString(): String {
        return key
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Correlation) return false
        if (key != other.key) return false
        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    internal fun value(value: String): Correlation {
        this.key = value
        return this
    }

    companion object {

        fun create(value: String?): Correlation? {
            return if (value != null) Correlation().value(value) else null
        }

    }

}

@Embeddable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
class Execution() {

    @Column(name = "STARTED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var startedAt: Date? = null
        internal set

    @Column(name = "FINISHED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var finishedAt: Date? = null
        internal set

}

@NoRepositoryBean
interface CommandStore<C>: CrudRepository<C, CommandId> {

    fun findByCorrelatedBy_AndExecutionFinishedAt_IsNull(correlation: Correlation): C?

    fun findFirstByName_AndIssuedBy_OrderByIssuedAtDesc(name: Name, issuedBy: CommandId): C?

    @Query("select c from StoredCommand c where c.correlatedToEvents like %:eventId%")
    fun findByCorrelatedToEvents_Containing(eventId: String): List<C>

}

enum class CommandStatus: MessageStatus {

    issued, forwarded, started, finished, processed

}
