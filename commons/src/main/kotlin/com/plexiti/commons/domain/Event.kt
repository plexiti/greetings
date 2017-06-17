package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.adapters.db.InMemoryEntityCrudRepository
import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.*
/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity
@Table(name="EVENTS")
@NamedQuery(name = "EventPublisher", query = "select e from EventEntity e where e.publishedAt is null")
class EventEntity(): AbstractMessageEntity<Event, EventId>() {

    @Embedded
    lateinit var aggregate: Event.EventAggregate
        private set

    @Embedded @AttributeOverride(name="value", column=Column(name="COMMAND_ID"))
    var commandId: CommandId? = null
        private set

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "RAISED_AT")
    lateinit var raisedAt: Date
        private set

    internal constructor(event: Event): this() {
        this.type = event.type
        this.origin = event.origin
        this.id = EventId(event.id)
        this.name = event.name
        this.definition = event.definition
        this.raisedAt = event.raisedAt
        this.aggregate = event.aggregate!!
        this.commandId =  if (event.commandId != null) CommandId(event.commandId) else null
        this.json = event.json
    }

    fun toEvent(): Event {
        return Event.toEvent(json)
    }

    fun <E: Event> toEvent(type: Class<E>): E {
        return Event.toEvent(json, type)
    }

    override fun setConsumed() {
        if (publishedAt == null) {
            publishedAt = Date()
        }
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
open class Event(): Message {

    override var type = MessageType.Event
    override val name =
        this::class.java.simpleName.substring(0,1).toLowerCase() +
            this::class.java.simpleName.substring(1)
    override var origin = context
    override lateinit var id: String; protected set
    override val definition = 0
    lateinit var raisedAt: Date
    var aggregate: EventAggregate? = null
    val commandId = if (Command.active() != null) Command.active()!!.id else null

    @JsonIgnore var json: String = ""
        @JsonIgnore get() {
            if (field == "")
                field = toJson(this)
            return field
        }
        @JsonIgnore internal set

    constructor(aggregate: Aggregate<*>?): this() {
        this.aggregate = if (aggregate != null) EventAggregate(aggregate) else null
        this.id = UUID.randomUUID().toString()
        this.raisedAt = Date()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(this::class.java)

        internal lateinit var context: String

        internal var repository: EventEntityRepository = InMemoryEventRepository()
            internal set

        fun <E: Event> raise(event: E): E {
            repository.save(EventEntity(event))
            logger.info("Raised ${event.json}")
            return event
        }

        internal fun toEvent(json: String): Event {
            val event = toEvent(json, Event::class.java)
            event.json = json
            return event
        }

        internal fun <E: Event> toEvent(json: String, type: Class<E>): E {
            val event = ObjectMapper().readValue(json, type)
            event.json = json
            return event
        }

        internal fun toJson(event: Event): String {
            return ObjectMapper().writeValueAsString(event)
        }

        fun findByAggregate(id: AggregateId): List<Event> {
            return repository.findByAggregate_Id(id.value).map {
                it.toEvent()
            }
        }

        fun findByAggregate(aggregate: Aggregate<*>): List<Event> {
            return findByAggregate(aggregate.id)
        }

        fun findOne(id: String): Event? {
            return repository.findOne(EventId(id))?.toEvent()
        }

    }

    @Embeddable
    class EventAggregate() {

        @Column(name = "AGG_ID", columnDefinition = "varchar(36)")
        lateinit var id: String
            private set

        @Column(name = "AGG_TYPE", columnDefinition = "varchar(128)")
        lateinit var type: String
            private set

        @Column(name = "AGG_VERSION")
        var version: Int = 0
            private set

        constructor(aggregate: Aggregate<*>): this() {
            id = aggregate.id.value
            type = aggregate::class.simpleName!!
            version = if (aggregate.isNew()) 0 else aggregate.version!! + 1
        }

    }

}

class EventId(value: String = ""): MessageId(value)

@Repository
interface EventEntityRepository: CrudRepository<EventEntity, EventId> {

    fun findByAggregate_Id(id: String): List<EventEntity>

}

@NoRepositoryBean
class InMemoryEventRepository: InMemoryEntityCrudRepository<EventEntity, EventId>(), EventEntityRepository {

    override fun findByAggregate_Id(id: String): List<EventEntity> {
        return findAll().filter { id == it.aggregate.id }
    }

}

@Component
private class EventInitialiser : ApplicationContextAware {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Event.context = context
        Event.repository = applicationContext!!.getBean(EventEntityRepository::class.java)
    }

}
