package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonIgnore
import com.plexiti.commons.application.CommandStatus.*
import com.plexiti.commons.domain.*
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */

open class Command: Message {

    lateinit var type: MessageType
        protected set
    override lateinit var context: Context
        protected set
    override lateinit var name: String
        protected set
    var definition: Int = 0
        protected set
    var forwardedAt: Date? = null
        protected set
    lateinit var issuedBy: Context
        protected set
    lateinit var issuedAt: Date
        protected set
    lateinit var correlation: String
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
class CommandEntity: AbstractMessageEntity<CommandId, CommandStatus>() {

    @Embedded @AttributeOverride(name="name", column = Column(name="ISSUED_BY"))
    lateinit var issuedBy: Context
        protected set

    @Column(name = "CORRELATION", length = 128)
    lateinit var correlation: String
        protected set

    @Column(name = "ISSUED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    var issuedAt = Date()
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
