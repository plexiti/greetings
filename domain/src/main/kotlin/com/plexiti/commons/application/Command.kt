package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonIgnore
import com.plexiti.commons.application.CommandStatus.*
import com.plexiti.commons.domain.*
import com.rabbitmq.client.Command
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
interface CommandInterface: MessageInterface {

    val issuedBy: Context
    val issuedAt: Date
    val correlation: String

}

open class Command: CommandInterface {

    override lateinit var type: MessageType
        protected set
    override lateinit var context: Context
        protected set
    override lateinit var name: String
        protected set
    override var definition: Int = 0
        protected set
    override var forwardedAt: Date? = null
        protected set
    override lateinit var issuedBy: Context
        protected set
    override lateinit var issuedAt: Date
        protected set
    override lateinit var correlation: String
        protected set

}

@Entity
@Table(name="COMMANDS")
@NamedQueries(
    NamedQuery(
        name = "CommandForwarding",
        query = "select c from CommandEntity c" // where c.status = com.plexiti.commons.application.CommandStatus.triggered"
    )
)
class CommandEntity: AbstractMessageEntity<CommandId, CommandStatus>(), CommandInterface {

    @Transient
    override val type = MessageType.Command

    @Embedded @AttributeOverride(name="name", column = Column(name="ISSUED_BY"))
    override lateinit var issuedBy: Context
        protected set

    @Column(name = "CORRELATION", length = 128)
    override lateinit var correlation: String
        protected set

    @Column(name = "ISSUED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    override var issuedAt = Date()
        protected set

    @JsonIgnore
    @Embedded @AttributeOverride(name="value", column = Column(name="TRIGGERED_BY"))
    var triggeredBy: EventId? = null
        @JsonIgnore get
        @JsonIgnore protected set

    @JsonIgnore
    @Column(name = "STARTED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    var startedAt: Date? = null
        protected set

    @JsonIgnore
    @Column(name = "FINISHED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    var finishedAt: Date? = null
        protected set

    @JsonIgnore
    @Embedded  @AttributeOverride(name="value", column = Column(name="FINISHED_BY"))
    var finishedBy: EventId? = null
        @JsonIgnore get
        @JsonIgnore protected set

    @Consumed
    fun transition(): CommandStatus {
        status = when (status) {
            triggered -> forwarded;
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

@Repository
interface CommandEntityRepository: CrudRepository<CommandEntity, CommandId>

enum class CommandStatus: MessageStatus {
    triggered, forwarded, started, finished
}
