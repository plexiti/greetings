package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonIgnore
import com.plexiti.commons.application.CommandStatus.*
import com.plexiti.commons.domain.AbstractMessageEntity
import com.plexiti.commons.domain.EventId
import com.plexiti.commons.domain.MessageId
import com.plexiti.commons.domain.MessageStatus
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity
@Table(name="COMMANDS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 16)
@NamedQueries(
    NamedQuery(
        name = "CommandForwarding",
        query = "select c from Command c where c.status = com.plexiti.commons.application.CommandStatus.triggered"
    )
)
open class Command : AbstractMessageEntity<CommandId, CommandStatus>() {

    @Column(name="SOURCE", length = 64)
    lateinit var source: String
        protected set

    override lateinit var status: CommandStatus

    @Column(name = "TRIGGERED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    var triggeredAt = Date()
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

    @JsonIgnore
    @Column(name = "CORRELATION_KEY", length = 128)
    lateinit var correlationKey: String
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
interface CommandRepository: CrudRepository<Command, CommandId>

enum class CommandStatus: MessageStatus {
    triggered, forwarded, started, finished
}
