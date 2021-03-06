package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.EventsMapConverter
import com.plexiti.commons.application.CommandStatus.*
import com.plexiti.commons.domain.*
import org.apache.camel.component.jpa.Consumed
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import java.io.Serializable
import java.util.*
import javax.persistence.*
import kotlin.collections.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    var issuedAt = Date()
        protected set

    companion object {

        private val logger = LoggerFactory.getLogger("com.plexiti.commons.application")

        private val executing = ThreadLocal<StoredCommand?>()
        private val raised: ThreadLocal<MutableList<Event>> = ThreadLocal.withInitial { ArrayList<Event>() }

        lateinit var types: Map<Name, KClass<out Command>>
        lateinit var names: Map<KClass<out Command>, Name>

        internal fun type(qName: Name): KClass<out Command> {
            return Command.types.get(qName) ?: throw IllegalArgumentException("Command type '${qName.qualified}' is not mapped to a local object type!")
        }

        var store = CommandStore()

        fun <C: Command> issue(command: C): C {
            try {
                return store.save(command)
            } finally {
                logger.info("Issued ${command.toJson()}")
            }
        }

        fun <C: Command> receive(command: C): C {
            try {
                return store.save(command)
            } finally {
                logger.info("Received ${command.toJson()}")
            }
        }

        fun issue(message: FlowIO): Command {
            val command = Command.type(message.command!!.name).createInstance()
            command.id = message.command!!.id
            command.init()
            val entity = store.save(command).internals()
            entity.issuedBy = message.flowId
            entity.executedBy = message.tokenId
            logger.info("Issued ${command.toJson()}")
            return command
        }

        internal fun setExecuting(command: StoredCommand) {
            executing.set(command)
        }

        internal fun getExecuting(): StoredCommand? {
            return executing.get()
        }

        internal fun getRaised(): MutableList<Event> {
            return raised.get()
        }

        internal fun unsetExecuting() {
            executing.set(null)
            raised.set(ArrayList())
        }

        fun fromJson(json: String): Command {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val name = node.get("name").textValue()
            val type = type(Name(name))
            return fromJson(json, type)
        }

        fun <C: Command> fromJson(json: String, type: KClass<C>): C {
            return ObjectMapper().readValue(json, type.java)
        }

    }

    open fun internals(): StoredCommand {
        return store.toEntity(this)!!
    }

    open fun init() {}

    open fun correlation(): Correlation {
        return Correlation.create(id.value)!!
    }

    open fun correlation(event: Event): Correlation? {
        return Correlation.create(event.internals().raisedByCommand?.value)
    }

    open fun trigger(event: Event): Command? {
        return null
    }

    override fun <T : Message> get(type: KClass<out T>): T? {
        return internals().get(type)
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
        name = "CommandQueuer",
        query = "select c from StoredCommand c where c.forwardedAt is null and type(c) = StoredCommand"
    ),
    NamedQuery(
        name = "FlowDocumentQueuer",
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

    @Column(name = "PROCESSED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var processedAt: Date? = null

    @Embedded
    var execution: Execution? = null

    fun getTriggeredBy(): EventId? {
        val trigger = eventsAssociated?.filter{ it.value == issued }?.keys
        return if (trigger != null && !trigger.isEmpty()) trigger.first() else null
    }

    @Embedded @AttributeOverride(name = "value", column = Column(name = "ISSUED_BY_FLOW", nullable = true, length=36))
    open var issuedBy: CommandId? = null

    @Embedded @AttributeOverride(name="value", column = Column(name="EXECUTED_BY_TOKEN", nullable = true))
    var executedBy: TokenId? = null

    @Embedded @AttributeOverride(name = "key", column = Column(name = "CORRELATED_BY_KEY", length = 128, nullable = false))
    lateinit var correlatedBy: Correlation

    @Column(name="EVENTS_ASSOCIATED", nullable = true, length=4096)
    @Convert(converter = EventsMapConverter::class)
    var eventsAssociated: Map<EventId, CommandStatus>? = null

    @Embedded @AttributeOverride(name="value", column = Column(name="VALUE_RETURNED", nullable = true, length = 40))
    var valueReturned: Hash? = null

    @Embedded
    var problemOccured: Problem? = null

    override fun <T : Message> get(type: KClass<out T>): T? {
        return Message.getFromFlow(type)
    }

    fun forward() {
        this.status = forwarded
        this.forwardedAt = Date()
    }

    open fun start() {
        this.status = started
        this.execution = Execution()
        this.execution?.startedAt = Date()
        Command.setExecuting(this)
        val flow = (if (issuedBy != null) Command.store.findOne(issuedBy)?.internals() else null) as StoredFlow?
        flow?.resume()
    }

    open fun finish() {
        this.status = finished
        this.execution = execution ?: Execution()
        this.execution?.finishedAt = Date()
        if (issuedBy == null) process()
        Command.unsetExecuting()
        val flow = (if (issuedBy != null) Command.store.findOne(issuedBy)?.internals() else null) as StoredFlow?
        flow?.hibernate()
    }

    fun process() {
        this.status = processed
        this.processedAt = Date()
    }

    fun correlate(result: Any) {
        if (result is Event) {
            val pair = result.id to this.status
            eventsAssociated = eventsAssociated?.plus(pair) ?: mapOf(pair)
            Flow.getExecuting()?.eventsAssociated = Flow.getExecuting()?.eventsAssociated?.plus(pair) ?: mapOf(pair)
            Command.getRaised().add(result)
        } else if (result is Value) {
            valueReturned = Hash(result)
        } else if (result is Problem) {
            problemOccured = result
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

    @Query("select c from StoredCommand c where c.eventsAssociated like %:eventId%")
    fun findByEventsAssociated_Containing(@Param("eventId") eventId: String): List<C>

}

enum class CommandStatus: MessageStatus {

    issued, forwarded, started, finished, processed

}
