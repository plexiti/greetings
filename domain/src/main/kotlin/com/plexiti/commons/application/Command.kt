package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.application.CommandStatus.*
import com.plexiti.commons.domain.*
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */

abstract class Command: Message {

    val type = MessageType.Command

    override var context = Command.context

    override val name = this::class.simpleName!!

    open val definition: Int = 0

    lateinit var id: CommandId
        protected set

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    var issuedAt: Date
        protected set

    var correlation: String
        protected set

    companion object {

        var context: Context = Context()

    }

    init {
        id = CommandId(UUID.randomUUID().toString())
        issuedAt = Date()
        correlation = id.value
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
        protected set

    @Embedded @AttributeOverride(name="name", column = Column(name="ISSUED_BY", nullable = false))
    var issuedBy = Command.context
        protected set

    @Column(name = "CORRELATION", length = 128, nullable = false)
    lateinit var correlation: String
        protected set

    @Embedded @AttributeOverride(name="value", column = Column(name="TRIGGERED_BY", nullable = true))
    var triggeredBy: EventId? = null
        protected set

    @Column(name = "STARTED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var startedAt: Date? = null
        protected set

    @Column(name = "FINISHED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var finishedAt: Date? = null
        protected set

    @Embedded @AttributeOverride(name="value", column = Column(name="FINISHED_BY", nullable = true))
    var finishedBy: EventId? = null
        protected set

    constructor(command: Command): this() {
        this.context = command.context
        this.name = command.name
        this.id = command.id
        this.issuedAt = command.issuedAt
        this.correlation = command.correlation
        this.json = ObjectMapper().writeValueAsString(command)
        this.status = issued
    }

    @Consumed
    fun consumed(): CommandStatus {
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

@NoRepositoryBean
interface CommandRepository<C>: CrudRepository<C, CommandId>

enum class CommandStatus: MessageStatus {
    issued, forwarded, started, finished
}
