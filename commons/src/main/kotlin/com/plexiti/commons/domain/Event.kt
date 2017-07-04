package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.adapters.db.EventRepository
import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.domain.EventEntity.EventAggregate
import com.plexiti.commons.domain.EventStatus.*
import com.plexiti.utils.scanPackageForAssignableClasses
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.*
import javax.persistence.*
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
open class Event(aggregate: Aggregate<*>? = null) : Message {

    override val type = MessageType.Event

    override var name = Name(name = this::class.simpleName!!)

    open val definition: Int = 0

    override lateinit var id: EventId
        protected set

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    lateinit var raisedAt: Date
        protected set

    lateinit var aggregate: EventAggregate

    @JsonIgnore
    lateinit var internals: EventEntity
        @JsonIgnore get
        @JsonIgnore set

    companion object {

        internal var types = scanPackageForAssignableClasses("com.plexiti", Event::class.java)
            .map { it.newInstance() as Event }
            .associate { Pair(it.name.qualified, it::class) }

        internal var repository = EventRepository()
        internal val executingCommand = ThreadLocal<Command?>()

        fun <E: Event> raise(event: E): E {
            val e = repository.save(event)
            e.internals.raisedDuring = executingCommand.get()?.id
            executingCommand.get()?.internals?.finish(e)
            return event
        }

        fun fromJson(json: String): Event {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val name = node.get("name").textValue()
            val type = repository.type(name)
            return fromJson(json, type)
        }

        fun <E: Event> fromJson(json: String, type: KClass<E>): E {
            val event =  ObjectMapper().readValue(json, type.java)
            event.construct()
            return event
        }

    }

    open fun construct() {}

    open fun flowCommand(name: Name): Command? {
        if (internals.flowId != null) {
            return Command.repository.findFirstByNameAndFlowIdOrderByIssuedAtDesc(name, internals.flowId!!)
        } else {
            throw IllegalStateException()
        }
    }

    open fun flowEvent(name: Name): Event? {
        if (internals.flowId != null) {
            return Event.repository.findFirstByNameAndFlowIdOrderByRaisedAtDesc(name, internals.flowId!!)
        } else {
            throw IllegalStateException()
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
@NamedQueries(
    NamedQuery(
        name = "EventForwarder",
        query = "select e from EventEntity e where e.forwardedAt is null"
    ),
    NamedQuery(
        name = "FlowEventForwarder",
        query = "select e from EventEntity e where e.consumedAt is not null and e.processedAt is null"
    )
)
class EventEntity(): AbstractMessageEntity<EventId, EventStatus>() {

    @Transient
    override val type = MessageType.Event

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

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PROCESSED_AT", nullable = true)
    var processedAt: Date? = null
        internal set

    @Embedded
    lateinit var aggregate: EventAggregate

    @Embedded @AttributeOverride(name="value", column = Column(name="FLOW_ID", nullable = true))
    var flowId: CommandId? = null
        internal set

    constructor(event: Event): this() {
        this.name = event.name
        this.id = event.id
        this.raisedAt = event.raisedAt
        this.aggregate = event.aggregate
        this.json = ObjectMapper().writeValueAsString(event)
        this.status = if (this.name.context == Name.default.context) raised else forwarded
    }

    fun forward() {
        this.status = forwarded
        this.forwardedAt = Date()
    }

    fun consume() {
        this.status = consumed
        this.consumedAt = Date()
    }

    fun process() {
        this.status = processed
        this.processedAt = Date()
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
    fun transition(): EventStatus {
        when (status) {
            raised -> forward()
            forwarded -> consume()
            consumed -> process()
            processed -> throw IllegalStateException()
        }
        return status
    }
    
}

class EventId(value: String = ""): MessageId(value)

@NoRepositoryBean
interface EventRepository<E>: CrudRepository<E, EventId> {

    fun findByAggregateId(id: String): List<E>

    fun findFirstByNameAndFlowIdOrderByRaisedAtDesc(name: Name, flowId: CommandId): E?

    fun findByRaisedDuringOrderByRaisedAtDesc(raisedDuring: CommandId): List<E>

}

enum class EventStatus: MessageStatus {

    raised, forwarded, consumed, processed

}
