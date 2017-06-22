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
interface EventInterface : MessageInterface {

    val raisedAt: Date

}

open class Event: EventInterface {

    override val type: MessageType = MessageType.Event
    override lateinit var context: Context
        protected set
    override lateinit var name: String
        protected set
    override var definition: Int = 0
        protected set
    override var forwardedAt: Date? = null
        protected set
    override lateinit var raisedAt: Date

}

@Entity
@Table(name="EVENTS")
@NamedQuery(
    name = "EventForwarding",
    query = "select e from EventEntity e" // where e.status = com.plexiti.commons.application.EventStatus.raised'"
)
class EventEntity(): AbstractMessageEntity<EventId, EventStatus>(), EventInterface {

    @Transient
    override val type = MessageType.Event

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "RAISED_AT")
    override lateinit var raisedAt: Date
        protected set

    @Embedded
    @AttributeOverride(name="value", column = Column(name="RAISED_BY"))
    var raisedBy: CommandId? = null
        protected set

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CONSUMED_AT")
    lateinit var consumedAt: Date
        protected set

    @Embedded
    lateinit var aggregate: EventAggregate

    constructor(aggregate: Aggregate<*>): this() {
        this.id = EventId(UUID.randomUUID().toString())
        this.aggregate = EventAggregate(aggregate)
    }

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
interface EventEntityRepository: CrudRepository<EventEntity, EventId> {

    fun findByAggregateId(id: String): List<EventInterface>

}

enum class EventStatus: MessageStatus {
    raised, forwarded, consumed
}
