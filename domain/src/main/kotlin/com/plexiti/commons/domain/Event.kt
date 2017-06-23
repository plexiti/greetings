package com.plexiti.commons.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.domain.EventEntity.EventAggregate
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
abstract class Event(aggregate: Aggregate<*>? = null) : Message {

    val type = MessageType.Event

    override var context = Event.context

    override val name = this::class.simpleName!!

    open val definition: Int = 0

    lateinit var id: EventId
        protected set

    lateinit var raisedAt: Date
        protected set

    lateinit var aggregate: EventAggregate

    companion object {
        var context: Context = Context("Test")
    }

    init {
        if (aggregate != null) {
            id = EventId(UUID.randomUUID().toString())
            raisedAt = Date()
            this.aggregate = EventAggregate(aggregate)
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Event) return false
        if (id != other.id) return false
        return true
    }

}

@Entity
@Table(name="EVENTS")
@NamedQuery(
    name = "EventForwarding",
    query = "select e from EventEntity e" // where e.status = com.plexiti.commons.application.EventStatus.raised'"
)
class EventEntity(): AbstractMessageEntity<EventId, EventStatus>() {

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "RAISED_AT", nullable = false)
    lateinit var raisedAt: Date
        protected set

    @Embedded
    @AttributeOverride(name="value", column = Column(name="RAISED_BY", nullable = true))
    var raisedBy: CommandId? = null
        protected set

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CONSUMED_AT", nullable = true)
    var consumedAt: Date? = null
        protected set

    @Embedded
    lateinit var aggregate: EventAggregate

    constructor(event: Event): this() {
        this.context = event.context
        this.name = event.name
        this.id = event.id
        this.raisedAt = event.raisedAt
        this.aggregate = event.aggregate
        this.json = ObjectMapper().writeValueAsString(event)
        this.status = EventStatus.raised
    }

    @Embeddable
    class EventAggregate() {

        @Column(name = "AGG_ID", length = 36, nullable = false)
        lateinit var id: String
            protected set

        @Column(name = "AGG_TYPE", length = 128, nullable = false)
        lateinit var type: String
            protected set

        @Column(name = "AGG_VERSION", nullable = false)
        var version: Int = 0
            protected set

        constructor(aggregate: Aggregate<*>): this() {
            id = aggregate.id.value
            type = aggregate::class.simpleName!!
            version = if (aggregate.isNew()) 0 else aggregate.version!! + 1
        }

    }

    @Consumed
    fun consumed(): EventStatus {
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

    internal fun qualifiedName(): String {
        return "${context.name}/${name}"
    }


}

class EventId(value: String = ""): MessageId(value)

@NoRepositoryBean
interface EventRepository<E>: CrudRepository<E, EventId> {
    fun findByAggregateId(id: String): List<E>
}

enum class EventStatus: MessageStatus {
    raised, forwarded, consumed
}
