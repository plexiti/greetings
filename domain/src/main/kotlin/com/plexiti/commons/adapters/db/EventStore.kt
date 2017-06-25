package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.domain.*
import com.plexiti.utils.scanPackageForAssignableClasses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component @NoRepositoryBean
class EventStore: EventRepository<Event>, ApplicationContextAware {

    @Value("\${com.plexiti.app.context}")
    private var context: String? = null

    @Autowired
    private var delegate: EventEntityRepository = InMemoryEventEntityRepository()

    internal var eventTypes: Map<String, KClass<out Event>> = emptyMap()

    internal fun type(qName: String): KClass<out Event> {
        return eventTypes.get(qName) ?: throw IllegalArgumentException("Event type '$qName' is not mapped to a local object type!")
    }

    private fun toEvent(entity: EventEntity?): Event? {
        if (entity != null) {
            val event = Event.fromJson(entity.json, type(entity.qname()))
            event.internals = entity
            return event
        }
        return null
    }

    private fun toEntity(event: Event?): EventEntity? {
        return if (event != null) (delegate.findOne(event.id) ?: EventEntity(event)) else null
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        Context.home = Context(context)
        Event.store = this
        eventTypes = scanPackageForAssignableClasses("com.plexiti", Event::class.java)
            .map { it.newInstance() as Event }
            .associate { Pair(it.qname(), it::class) }
    }

    override fun exists(id: EventId?): Boolean {
        return delegate.exists(id)
    }

    override fun findOne(id: EventId?): Event? {
        return toEvent(delegate.findOne(id))
    }

    fun eventId(json: String): EventId? {
        try {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val id =  node.get("id").textValue()
            return if (id != null) EventId(id) else null
        } catch (ex: JsonMappingException) {
            return null
        }
    }

    fun findOne(json: String): Event? {
        return findOne(eventId(json))
    }

    override fun findAll(): MutableIterable<Event> {
        return delegate.findAll().mapTo(ArrayList(), { toEvent(it)!! })
    }

    override fun findAll(ids: MutableIterable<EventId>?): MutableIterable<Event> {
        return delegate.findAll(ids).mapTo(ArrayList(), { toEvent(it)!! })
    }

    override fun findByAggregateId(id: String): List<Event> {
        return delegate.findByAggregateId(id).map { toEvent(it)!! }
    }

    override fun <S : Event?> save(event: S): S {
        @Suppress("unchecked_cast")
        return toEvent(delegate.save(toEntity(event))) as S
    }

    override fun <S : Event?> save(events: MutableIterable<S>?): MutableIterable<S> {
        return events?.mapTo(ArrayList(), { save(it) })!!
    }

    override fun count(): Long {
        return delegate.count()
    }

    override fun delete(entities: MutableIterable<Event>?) {
        delegate.delete(entities?.map { toEntity(it) })
    }

    override fun delete(event: Event?) {
        delegate.delete(toEntity(event))
    }

    override fun delete(id: EventId?) {
        delegate.delete(id)
    }

    override fun deleteAll() {
        delegate.deleteAll()
    }

}

@Repository
internal interface EventEntityRepository: EventRepository<EventEntity>
