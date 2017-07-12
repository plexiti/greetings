package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.application.Flow
import com.plexiti.commons.application.FlowIO
import com.plexiti.commons.domain.EventStatus.*
import com.plexiti.commons.domain.StoredEvent.*
import org.apache.camel.component.jpa.Consumed
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import java.util.*
import javax.persistence.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
open class Event() : Message {

    constructor(aggregate: Aggregate<*>? = null): this() {
        if (aggregate != null) this.aggregate = EventAggregate(aggregate)
    }

    constructor(name: Name): this() {
        this.name = name
    }

    override val type = MessageType.Event

    override var name = Name(name = this::class.simpleName!!)

    open val definition: Int = 0

    override lateinit var id: EventId
        protected set

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    var raisedAt: Date
        protected set

    var aggregate: EventAggregate? = null

    init {
        id = EventId(UUID.randomUUID().toString())
        raisedAt = Date()
    }

    companion object {

        lateinit internal var types: Map<Name, KClass<out Event>>

        lateinit var names: Map<KClass<out Event>, Name>

        var store = EventStore()

        internal fun type(qName: Name): KClass<out Event> {
            return types.get(qName) ?: throw IllegalArgumentException("Event type '$qName' is not mapped to a local object type!")
        }

        fun <E: Event> raise(event: E): E {
            val entity = store.save(event).internals()
            entity.raisedByFlow = Flow.getExecuting()?.id
            entity.raisedByCommand = Command.getExecuting()?.id
            Flow.getExecuting()?.correlate(event)
            Command.getExecuting()?.correlate(event)
            return event
        }

        fun raise(message: FlowIO): Event {
            val event = type(message.event!!.name).createInstance()
            event.init()
            return raise(event);
        }

        fun fromJson(json: String): Event {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val name = node.get("name").textValue()
            val type = type(Name(name))
            return fromJson(json, type)
        }

        fun <E: Event> fromJson(json: String, type: KClass<E>): E {
            return ObjectMapper().readValue(json, type.java)
        }

    }

    open fun internals(): StoredEvent {
        return store.toEntity(this)!!
    }

    open fun init() {}

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

    override fun <T : Message> get(type: KClass<out T>): T? {
        return internals().get(type)
    }

}

@Entity
@Table(name="EVENTS")
@NamedQueries(
    NamedQuery(
        name = "EventPublisher",
        query = "select e from StoredEvent e where e.forwardedAt is null"
    ),
    NamedQuery(
        name = "FlowEventQueuer",
        query = "select e from StoredEvent e where e.consumedAt is not null and e.processedAt is null"
    )
)
class StoredEvent(): StoredMessage<EventId, EventStatus>() {

    @Transient
    override val type = MessageType.Event

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "RAISED_AT", nullable = false)
    lateinit var raisedAt: Date

    @Embedded
    @AttributeOverride(name="value", column = Column(name="RAISED_BY_COMMAND", nullable = true, length = 36))
    var raisedByCommand: CommandId? = null

    @Embedded
    @AttributeOverride(name="value", column = Column(name="RAISED_BY_FLOW", nullable = true, length = 36))
    var raisedByFlow: CommandId? = null

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CONSUMED_AT", nullable = true)
    var consumedAt: Date? = null

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PROCESSED_AT", nullable = true)
    var processedAt: Date? = null

    @Embedded
    var aggregate: EventAggregate? = null

    constructor(event: Event): this() {
        this.name = event.name
        this.id = event.id
        this.raisedAt = event.raisedAt
        this.aggregate = event.aggregate
        this.json = ObjectMapper().writeValueAsString(event)
        this.status = if (this.name.context == Name.context) raised else forwarded
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

    override fun <T : Message> get(type: KClass<out T>): T? {
        return Message.getFromFlow(type)
    }

    @Embeddable
    class EventAggregate() {

        @Column(name = "AGGREGATE_ID", length = 36, nullable = true)
        lateinit var id: String
            protected set

        @Column(name = "AGGREGATE_NAME", length = 128, nullable = true)
        lateinit var name: String
            protected set

        @Column(name = "AGGREGATE_VERSION", nullable = true)
        var version: Int = 0
            protected set

        constructor(aggregate: Aggregate<*>): this() {
            id = aggregate.id.value
            name = aggregate::class.simpleName!!
            version = aggregate.version + 1
        }

    }

    @Consumed
    fun consumed() {
        when (status) {
            raised -> forward()
            consumed -> process()
            else -> throw IllegalStateException()
        }
    }

}

class EventId(value: String = ""): MessageId(value)

@NoRepositoryBean
interface EventStore<E>: CrudRepository<E, EventId> {

    fun findByAggregateId(id: String): List<E>

    @Query( "select e from StoredEvent e where e.name = :name and (e.id in :ids) order by e.raisedAt desc")
    fun findFirstByName_OrderByRaisedAtDesc(@Param("name") name: Name, @Param("ids") ids: MutableIterable<EventId>): List<E>

    fun findByRaisedByCommand_OrderByRaisedAtDesc(raisedBy: CommandId): List<E>

    fun findByRaisedByFlow_OrderByRaisedAtDesc(raisedBy: CommandId): List<E>

    @Query("select e from StoredEvent e where e.id in :ids order by e.raisedAt desc")
    fun findAll_OrderByRaisedAtDesc(@Param("ids") ids: MutableIterable<EventId>): List<E>

}

enum class EventStatus: MessageStatus {

    raised, forwarded, consumed, processed

}
