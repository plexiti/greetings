package com.plexiti.commons.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.adapters.db.InMemoryEntityCrudRepository
import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandId
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.data.repository.CrudRepository
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
class EventEntity(): AbstractMessageEntity<EventId>() {

    @Embedded
    lateinit var aggregate: Event.Aggregate
        private set

    @Embedded @AttributeOverride(name="value", column=Column(name="COMMAND_ID"))
    var commandId: CommandId? = null
        private set

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "RAISED_AT")
    lateinit var raisedAt: Date
        private set

    internal constructor(event: Event): this() {
        this.message = event.message
        this.origin = event.origin
        this.id = EventId(event.id)
        this.type = event.type
        this.definition = event.definition
        this.raisedAt = event.raisedAt
        this.aggregate = event.aggregate
        this.commandId = if (Command.active() != null) CommandId(Command.active()!!.id) else null
        this.json = ObjectMapper().writeValueAsString(event)
    }

}

abstract class Event(aggregate: com.plexiti.commons.domain.Aggregate<*>): Message {

    override var message = MessageType.Event
    override val id = UUID.randomUUID().toString()
    override var origin: String? = null
    override val type = this::class.java.simpleName
    val raisedAt = Date()
    val aggregate = Aggregate(aggregate)

    companion object {

        var context: String? = null

        var repository: CrudRepository<EventEntity, EventId> = InMemoryEntityCrudRepository<EventEntity, EventId>()
            internal set

        fun <E: Event> raise(event: E): E {
            event.origin = context
            repository.save(EventEntity(event))
            return event
        }

    }

    @Embeddable
    class Aggregate() {

        @Column(name = "AGG_ID", columnDefinition = "varchar(36)")
        lateinit var id: String
            private set
        @Column(name = "AGG_TYPE", columnDefinition = "varchar(128)")
        lateinit var type: String
            private set
        @Column(name = "AGG_VERSION")
        var version: Int = 0
            private set

        constructor(aggregate: com.plexiti.commons.domain.Aggregate<*>): this() {
            id = aggregate.id.value
            type = aggregate::class.simpleName!!
            version = if (aggregate.isNew()) 0 else aggregate.version!! + 1
        }

    }

}

class EventId(value: String = ""): MessageId(value)

@Repository
interface EventEntityRepository: CrudRepository<EventEntity, EventId>

@Component
private class EventRaiserInitialiser: ApplicationContextAware {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Event.context = context
        Event.repository = applicationContext!!.getBean(EventEntityRepository::class.java)
    }

}
