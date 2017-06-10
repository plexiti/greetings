package com.plexiti.commons.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandId
import org.apache.camel.component.jpa.Consumed
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.*
import kotlin.collections.HashMap

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

    @Column(name="TYPE", columnDefinition = "varchar(128)")
    lateinit var type: String
        private set

    @Column(name="DEFINITION")
    var definition: Int = 0

    @Lob
    @Column(name="JSON", columnDefinition = "text")
    lateinit var json: String
        private set

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PUBLISHED_AT")
    var publishedAt: Date? = null
        private set

    internal constructor(event: Event): this() {
        this.id = EventId(event.id)
        this.type = event.type
        this.definition = event.definition
        this.raisedAt = event.raisedAt
        this.aggregate = event.aggregate
        this.commandId = Command.active()?.id
        this.json = ObjectMapper().writeValueAsString(event)
    }

    @Consumed
    fun setPublished() {
        if (publishedAt == null)
            publishedAt = Date()
    }

    fun isPublished(): Boolean {
        return publishedAt != null
    }

}

abstract class Event(aggregate: com.plexiti.commons.domain.Aggregate<*>) {

    val id = UUID.randomUUID().toString()
    val type = this::class.java.simpleName
    open abstract val definition: Int
    val raisedAt = Date()
    val aggregate = Aggregate(aggregate)

    companion object {

        internal var repository: EventEntityRepository? = null
        private val store = HashMap<String, Event>()

        fun raise(event: Event) {
            if (store.containsKey(event.id))
                throw IllegalStateException()
            if (repository != null)
                repository!!.save(EventEntity(event))
            else
                store.put(event.id, event)
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

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Event.repository = applicationContext!!.getBean(EventEntityRepository::class.java)
    }

}
