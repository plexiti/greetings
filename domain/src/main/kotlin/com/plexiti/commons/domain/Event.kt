package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.domain.EventEntity.EventAggregate
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.*
import javax.persistence.*
import kotlin.reflect.KClass

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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    lateinit var raisedAt: Date
        protected set

    lateinit var aggregate: EventAggregate

    companion object {

        internal var context = Context()
        internal var store = EventStore()

        fun <E: Event> raise(event: E): E {
            event.id = EventId(UUID.randomUUID().toString())
            event.raisedAt = Date()
            return store.save(event)
        }

        fun consume(json: String): Event? {
            val eventId = store.eventId(json)
            if (eventId != null) {
                val event = store.findOne(eventId) ?: Event.fromJson(json)
                return store.save(event) // TODO mark as consumed
            }
            return null
        }

        fun fromJson(json: String): Event {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val qName =  node.get("context").textValue() + "/" + node.get("name").textValue()
            val type = store.type(qName)
            return ObjectMapper().readValue(json, type.java)
        }

        fun <E: Event> fromJson(json: String, type: KClass<E>): E {
            return ObjectMapper().readValue(json, type.java)
        }

   }

    init {
        if (aggregate != null) {
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

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
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
    @AttributeOverride(name="value", column = Column(name="RAISED_DURING", nullable = true))
    var raisedDuring: CommandId? = null
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
    
}

class EventId(value: String = ""): MessageId(value)

@NoRepositoryBean
interface EventRepository<E>: CrudRepository<E, EventId> {
    fun findByAggregateId(id: String): List<E>
}

enum class EventStatus: MessageStatus {
    raised, forwarded, consumed
}
