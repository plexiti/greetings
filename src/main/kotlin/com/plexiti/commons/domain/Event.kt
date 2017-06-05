package com.plexiti.commons.domain

import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.application.Commands
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
@Entity @Inheritance
@Table(name="EVENTS")
@DiscriminatorColumn(name="type", columnDefinition = "varchar(128)", discriminatorType = DiscriminatorType.STRING)
open class Event(aggregate:  Aggregate<*>? = null): AbstractMessage<EventId>(EventId()) {

    @Embedded
    lateinit var aggregate: ReferencedAggregate private set

    @Embedded @AttributeOverride(name="value", column=Column(name="COMMAND_ID"))
    lateinit var commandId: CommandId; private set

    init {
        if (aggregate != null) {
            this.aggregate = ReferencedAggregate(aggregate)
            this.commandId = Commands.active().id!!
        }
    } protected

    @Column(name = "RAISED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    var raisedAt = Date(); private set

    @Embeddable
    class ReferencedAggregate(aggregate: Aggregate<*>? = null) {

        @Column(name = "AGG_ID", columnDefinition = "varchar(36)")
        lateinit var id: String private set
        @Column(name = "AGG_TYPE", columnDefinition = "varchar(128)")
        lateinit var type: String private set
        @Column(name = "AGG_VERSION")
        var version: Int? = null; private set

        init {
            if (aggregate != null) {
                id = aggregate.id!!.value
                type = aggregate::class.simpleName!!
                version = if (aggregate.isNew()) 0 else aggregate.version!! + 1
            }
        }

    }

}

class EventId(value: String? = null): MessageId(value)

@Repository
interface EventRepository: CrudRepository<Event, EventId>

object Events {

    lateinit var eventRepository: EventRepository

    fun raise(event: Event) {
        eventRepository.save(event)
    }

}

@Component
private class EventRaiserInitialiser: ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Events.eventRepository = applicationContext!!.getBean(EventRepository::class.java)
    }

}
