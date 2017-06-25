package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.adapters.db.EventRepository
import com.plexiti.commons.application.Command
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

    override var context = Context.home

    override val name = this::class.simpleName!!

    open val definition: Int = 0

    lateinit var id: EventId
        protected set

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    lateinit var raisedAt: Date
        protected set

    lateinit var aggregate: EventAggregate

    @JsonIgnore
    internal lateinit var internals: EventEntity
        @JsonIgnore get
        @JsonIgnore set

    companion object {

        internal var store = EventRepository()
        internal val executingCommand = ThreadLocal<Command?>()

        fun <E: Event> raise(event: E): E {
            val event = store.save(event)
            event.internals.raisedDuring = executingCommand.get()?.id
            return event
        }

        fun fromJson(json: String): Event {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val qName =  node.get("context").textValue() + "/" + node.get("name").textValue()
            val type = store.type(qName)
            return fromJson(json, type)
        }

        fun <E: Event> fromJson(json: String, type: KClass<E>): E {
            return ObjectMapper().readValue(json, type.java)
        }

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
        internal set

    @Embedded
    @AttributeOverride(name="value", column = Column(name="RAISED_DURING", nullable = true))
    var raisedDuring: CommandId? = null
        internal set

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CONSUMED_AT", nullable = true)
    var consumedAt: Date? = null
        internal set

    @Embedded
    lateinit var aggregate: EventAggregate

    constructor(event: Event): this() {
        this.context = event.context
        this.name = event.name
        this.id = event.id
        this.raisedAt = event.raisedAt
        this.aggregate = event.aggregate
        this.json = ObjectMapper().writeValueAsString(event)
        this.status = if (this.context == Context.home) EventStatus.raised else EventStatus.forwarded
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
    fun transitioned(): EventStatus {
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
