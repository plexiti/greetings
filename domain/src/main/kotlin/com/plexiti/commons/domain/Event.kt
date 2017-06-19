package com.plexiti.commons.domain

import com.plexiti.commons.application.CommandId
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity
@Table(name="EVENTS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 16)
@NamedQuery(
    name = "EventForwarding",
    query = "select e from Event e where e.status = com.plexiti.commons.application.EventStatus.raised'"
)
open class Event: AbstractMessageEntity<EventId, EventStatus>() {

    @Column(name="TARGET", length = 64)
    lateinit var target: String
        protected set

    override lateinit var status: EventStatus

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "RAISED_AT")
    lateinit var raisedAt: Date
        private set

    @Embedded
    @AttributeOverride(name="value", column = Column(name="RAISED_BY"))
    var raisedBy: CommandId? = null
        private set

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CONSUMED_AT")
    lateinit var consumedAt: Date
        private set

    @Embedded
    lateinit var aggregate: EventAggregate
        private set

    @Embeddable
    class EventAggregate() {

        @Column(name = "AGG_ID", length = 36)
        lateinit var id: String
            protected set

        @Column(name = "AGG_TYPE", length = 128)
        lateinit var type: String
            protected set

        @Column(name = "AGG_VERSION")
        var version: Int = 0
            protected set

        constructor(aggregate: Aggregate<*>): this() {
            id = aggregate.id.value
            type = aggregate::class.simpleName!!
            version = if (aggregate.isNew()) 0 else aggregate.version!! + 1
        }

    }

    @Consumed
    fun transition(): EventStatus {
        status = when (status) {
            EventStatus.raised -> EventStatus.forwarded;
            EventStatus.forwarded -> EventStatus.consumed
            EventStatus.consumed -> throw IllegalStateException()
        }
        when (status) {
            EventStatus.forwarded -> forwardedAt = Date()
            EventStatus.consumed -> consumedAt = Date()
        }
        return status
    }

}

class EventId(value: String = ""): MessageId(value)

@Repository
interface EventRepository: CrudRepository<Event, EventId> {

    fun findByAggregateId(id: String): List<Event>

}

enum class EventStatus: MessageStatus {
    raised, forwarded, consumed
}
