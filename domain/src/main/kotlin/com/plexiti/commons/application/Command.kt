package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.adapters.db.CommandRepository
import com.plexiti.commons.application.CommandStatus.*
import com.plexiti.commons.domain.*
import com.plexiti.utils.scanPackageForAssignableClasses
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.io.Serializable
import java.util.*
import javax.persistence.*
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
open class Command: Message {

    override val type = MessageType.Command

    override var name = Name(name = this::class.simpleName!!)

    open val definition: Int = 0

    override lateinit var id: CommandId
        protected set

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    lateinit var issuedAt: Date
        protected set

    @JsonIgnore
    lateinit var internals: CommandEntity
        @JsonIgnore get
        @JsonIgnore set

    companion object {

        internal var types = scanPackageForAssignableClasses("com.plexiti", Command::class.java)
            .filter { it != Flow::class.java && it != FlowCommand::class.java }
            .map { it.newInstance() as Command }
            .associate { Pair(it.name.qualified, it::class) }

        internal var repository = CommandRepository()

        fun <C: Command> issue(command: C): C {
            command.id = CommandId(UUID.randomUUID().toString())
            command.issuedAt = Date()
            return repository.save(command)
        }

        fun fromJson(json: String): Command {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val name = node.get("name").textValue()
            val type = repository.type(name)
            return fromJson(json, type)
        }

        fun <C: Command> fromJson(json: String, type: KClass<C>): C {
            val command =  ObjectMapper().readValue(json, type.java)
            command.construct()
            return command
        }

    }

    open fun construct() {}

    open fun flowCommand(name: Name): Command? {
        if (internals.flowId != null) {
            return Command.repository.findFirstByNameAndFlowIdOrderByIssuedAtDesc(name, internals.flowId!!)
        } else {
            throw IllegalStateException()
        }
    }

    open fun flowEvent(name: Name): Event? {
        if (internals.flowId != null) {
            return Event.repository.findFirstByNameAndFlowIdOrderByRaisedAtDesc(name, internals.flowId!!)
        } else {
            throw IllegalStateException()
        }
    }

    open fun correlation(): Correlation {
        return Correlation.create(id.value)!!
    }

    open fun correlation(event: Event): Correlation? {
        return Correlation.create(event.internals.raisedDuring?.value)
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
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue(MessageType.Discriminator.command)
@NamedQueries(
    NamedQuery(
        name = "CommandForwarder",
        query = "select c from CommandEntity c where c.forwardedAt is null and type(c) = CommandEntity"
    ),
    NamedQuery(
        name = "FlowCommandForwarder",
        query = "select c from CommandEntity c where c.execution.finishedAt is not null and c.processedAt is null"
    )
)
open class CommandEntity(): AbstractMessageEntity<CommandId, CommandStatus>() {

    constructor(command: Command) : this() {
        this.name = command.name
        this.id = command.id
        this.issuedAt = command.issuedAt
        this.correlation = command.correlation()
        this.json = ObjectMapper().writeValueAsString(command)
        this.status = if (this.name.context == Name.default.context) issued else forwarded
    }

    @Transient
    override val type = MessageType.Command

    @Column(name = "ISSUED_AT", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    var issuedAt = Date()
        internal set

    @Column(name = "COMPLETED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var processedAt: Date? = null
        internal set

    @Embedded @AttributeOverride(name = "value", column = Column(name = "CORRELATION", length = 128, nullable = false))
    lateinit var correlation: Correlation
        internal set

    @Embedded @AttributeOverride(name = "value", column = Column(name = "TRIGGERED_BY", nullable = true))
    var triggeredBy: EventId? = null
        internal set

    @Embedded @AttributeOverride(name="value", column = Column(name="FINISHED_BY", nullable = true))
    var finishedBy: EventId? = null
        internal set

    @Embedded @AttributeOverride(name = "value", column = Column(name = "FLOW_ID", nullable = true))
    var flowId: CommandId? = null
        internal set

    @Embedded @AttributeOverride(name="value", column = Column(name="TOKEN_ID", nullable = true))
    var tokenId: TokenId? = null
        internal set

    @Embedded @AttributeOverride(name="value", column = Column(name="DOCUMENT_ID", nullable = true))
    var documentId: DocumentId? = null
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
        this.execution.startedAt = Date()
    }

    fun process() {
        this.status = processed
        this.processedAt = Date()
    }

    fun finish(result: Any) {
        if (result is Event) {
            val event = result
            execution.finishedAt = event.raisedAt
            finishedBy = event.id
        } else if (result is Problem) {
            problem = result
            execution.finishedAt = result.occuredAt
            finishedBy = null
        } else if (result is Document) {
            execution.finishedAt = Date()
            documentId = DocumentId(result)
        }
        if (tokenId != null) {
            status = finished
        } else {
            status = processed
            processedAt = execution.finishedAt
        }
    }

    @Consumed
    fun forwarded(): CommandStatus {
        when (status) {
            issued -> forward()
            finished -> process()
            else -> IllegalStateException()
        }
        return status
    }

}

open class CommandId(value: String = ""): MessageId(value)

@Embeddable
class Correlation : Serializable {

    @Column(name = "KEY", length = 128, nullable = false)
    lateinit var value: String
        @JsonValue get
        @JsonValue private set

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Correlation) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    internal fun value(value: String): Correlation {
        this.value = value
        return this
    }

    companion object {

        fun create(value: String?): Correlation? {
            return if (value != null) Correlation().value(value) else null
        }

    }

}

@NoRepositoryBean
interface CommandRepository<C>: CrudRepository<C, CommandId> {

    fun findByCorrelationAndExecutionFinishedAtIsNull(correlation: Correlation): C?

    fun findFirstByNameAndFlowIdOrderByIssuedAtDesc(name: Name, flowId: CommandId): C?

}

enum class CommandStatus: MessageStatus {

    issued, forwarded, started, finished, processed

}
