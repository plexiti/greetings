package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.domain.Name
import com.plexiti.commons.domain.*
import com.plexiti.commons.domain.EventStore
import com.plexiti.utils.scanPackageForClassNames
import com.plexiti.utils.scanPackageForNamedClasses
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
class EventStore : EventStore<Event>, ApplicationContextAware {

    init { init() }

    private fun init() {
        types = scanPackageForNamedClasses("com.plexiti", Event::class)
        names = scanPackageForClassNames("com.plexiti", Event::class)
    }

    @Value("\${com.plexiti.app.context}")
    private var context = Name.context

    @Autowired
    private var delegate: StoredEventStore = InMemoryStoredEventStore()

    lateinit internal var types: Map<Name, KClass<out Event>>
    lateinit internal var names: Map<KClass<out Event>, Name>

    internal fun type(qName: Name): KClass<out Event> {
        return types.get(qName) ?: throw IllegalArgumentException("Event type '$qName' is not mapped to a local object type!")
    }

    internal fun toEvent(stored: StoredEvent?): Event? {
        return if (stored != null) Event.fromJson(stored.json, type(stored.name)) else null
    }

    internal fun toEntity(event: Event?): StoredEvent? {
        return if (event != null) (delegate.findOne(event.id) ?: StoredEvent(event)) else null
    }

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Event.store = this
        Name.context = context; init()
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

    override fun findFirstByNameAndFlowIdOrderByRaisedAtDesc(name: Name, flowId: CommandId): Event? {
        return toEvent(delegate.findFirstByNameAndFlowIdOrderByRaisedAtDesc(name, flowId))
    }

    override fun findByRaisedDuringOrderByRaisedAtDesc(raisedDuring: CommandId): List<Event> {
        return delegate.findByRaisedDuringOrderByRaisedAtDesc(raisedDuring).map { toEvent(it)!! }
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
internal interface StoredEventStore : EventStore<StoredEvent>

@NoRepositoryBean
class InMemoryStoredEventStore : InMemoryEntityCrudRepository<StoredEvent, EventId>(), StoredEventStore {

    override fun findByAggregateId(id: String): List<StoredEvent> {
        return findAll().filter { id == it.aggregate.id }
    }

    override fun findFirstByNameAndFlowIdOrderByRaisedAtDesc(name: Name, flowId: CommandId): StoredEvent? {
        return findAll().sortedByDescending { it.raisedAt }.first { it.name == name && it.flowId == flowId }
    }

    override fun findByRaisedDuringOrderByRaisedAtDesc(raisedDuring: CommandId): List<StoredEvent> {
        return findAll().sortedByDescending { it.raisedAt }.filter { it.raisedDuring == raisedDuring }
    }

}
