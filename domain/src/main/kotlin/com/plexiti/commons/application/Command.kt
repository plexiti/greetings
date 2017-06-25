package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.application.CommandStatus.*
import com.plexiti.commons.domain.*
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

abstract class Command: Message {

    val type = MessageType.Command

    override var context = Context.home

    override val name = this::class.simpleName!!

    open val definition: Int = 0

    lateinit var id: CommandId
        protected set

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    lateinit var issuedAt: Date
        protected set

    @JsonIgnore
    internal lateinit var internals: CommandEntity
        @JsonIgnore get
        @JsonIgnore set

    companion object {

        internal var store = CommandStore()

        fun <C: Command> issue(command: C): C {
            command.id = CommandId(UUID.randomUUID().toString())
            command.issuedAt = Date()
            return store.save(command)
        }

        fun fromJson(json: String): Command {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val qName = node.get("context").textValue() + "/" + node.get("name").textValue()
            val type = store.type(qName)
            return fromJson(json, type)
        }

        fun <C: Command> fromJson(json: String, type: KClass<C>): C {
            return ObjectMapper().readValue(json, type.java)
        }

    }

    open fun finishKey(): CorrelationKey {
        return CorrelationKey.create(id.value)!!
    }

    open fun triggerBy(event: Event): Command? {
        return null
    }

    open fun finishKey(event: Event): CorrelationKey? {
        return CorrelationKey.create(event.internals.raisedDuring?.value)
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
@NamedQueries(
    NamedQuery(
        name = "CommandForwarding",
        query = "select c from CommandEntity c" // where c.status = com.plexiti.commons.application.CommandStatus.issued"
    )
)
class CommandEntity(): AbstractMessageEntity<CommandId, CommandStatus>() {

    @Column(name = "ISSUED_AT", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    var issuedAt = Date()
        internal set

    @Embedded @AttributeOverride(name="name", column = Column(name="ISSUED_BY", nullable = false))
    var issuedBy = Context.home
        internal set

    @Embedded @AttributeOverride(name="value", column = Column(name = "FINISH_KEY", length = 128, nullable = false))
    lateinit var finishKey: CorrelationKey
        internal set

    @Embedded @AttributeOverride(name="value", column = Column(name="TRIGGERED_BY", nullable = true))
    var triggeredBy: EventId? = null
        internal set

    @Column(name = "STARTED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var startedAt: Date? = null
        internal set

    @Column(name = "FINISHED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var finishedAt: Date? = null
        internal set

    @Embedded @AttributeOverride(name="value", column = Column(name="FINISHED_BY", nullable = true))
    var finishedBy: EventId? = null
        internal set

    constructor(command: Command): this() {
        this.context = command.context
        this.name = command.name
        this.id = command.id
        this.issuedAt = command.issuedAt
        this.finishKey = command.finishKey()
        this.json = ObjectMapper().writeValueAsString(command)
        this.status = if (this.context == Context.home) issued else forwarded
    }

    @Consumed
    fun transitioned(): CommandStatus {
        status = when (status) {
            issued -> forwarded;
            forwarded -> started
            started -> finished
            finished -> throw IllegalStateException()
        }
        when (status) {
            forwarded -> forwardedAt = Date()
            started -> startedAt = Date()
            finished -> finishedAt = Date()
        }
        return status
    }

}

class CommandId(value: String = ""): MessageId(value)

@Embeddable
class CorrelationKey: Serializable {

    @Column(name = "KEY", length = 128, nullable = false)
    lateinit var value: String
        @JsonValue get
        @JsonValue private set

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CorrelationKey) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    internal fun value(value: String): CorrelationKey {
        this.value = value
        return this
    }

    companion object {

        fun create(value: String?): CorrelationKey? {
            return if (value != null) CorrelationKey().value(value) else null
        }

    }

}

@NoRepositoryBean
interface CommandRepository<C>: CrudRepository<C, CommandId> {

    fun findByFinishKey(finishKey: CorrelationKey): C?

}

enum class CommandStatus: MessageStatus {
    issued, forwarded, started, finished
}
