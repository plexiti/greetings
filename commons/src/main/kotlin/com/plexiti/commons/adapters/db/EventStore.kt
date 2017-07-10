package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.application.Flow
import com.plexiti.commons.application.FlowIO
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
    lateinit var names: Map<KClass<out Event>, Name>

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

    override fun findFirstByName_OrderByRaisedAtDesc(name: Name, ids: MutableIterable<EventId>): List<Event> {
        return delegate.findFirstByName_OrderByRaisedAtDesc(name, ids).map { toEvent(it)!! }
    }

    override fun findByRaisedByFlow_OrderByRaisedAtDesc(raisedBy: CommandId): List<Event> {
        return delegate.findByRaisedByFlow_OrderByRaisedAtDesc(raisedBy).map { toEvent(it)!! }
    }

    override fun findByRaisedByCommand_OrderByRaisedAtDesc(raisedBy: CommandId): List<Event> {
        return delegate.findByRaisedByCommand_OrderByRaisedAtDesc(raisedBy).map { toEvent(it)!! }
    }

    override fun findAll_OrderByRaisedAtDesc(ids: MutableIterable<EventId>): List<Event> {
        return delegate.findAll_OrderByRaisedAtDesc(ids).map { toEvent(it)!! }
    }

    override fun <S : Event?> save(event: S): S {
        val entity = toEntity(event)!!
        entity.raisedByFlow = Flow.getExecuting()?.id
        entity.raisedByCommand = Command.getExecuting()?.id
        delegate.save(entity)
        val e = toEvent(entity)!!
        Flow.getExecuting()?.correlate(e)
        Command.getExecuting()?.correlate(e)
        return event
    }

    fun save(message: FlowIO): Event {
        val event = save(message.event)!!
        event.construct()
        event.internals().json = event.toJson()
        return event
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
interface StoredEventStore : EventStore<StoredEvent>

@NoRepositoryBean
class InMemoryStoredEventStore : InMemoryEntityCrudRepository<StoredEvent, EventId>(), StoredEventStore {

    override fun findByAggregateId(id: String): List<StoredEvent> {
        return findAll().filter { id == it.aggregate?.id }
    }

    override fun findFirstByName_OrderByRaisedAtDesc(name: Name, ids: MutableIterable<EventId>): List<StoredEvent> {
        return findAll().filter { it.name == name && ids.contains(it.id) }.sortedByDescending { it.raisedAt }
    }

    override fun findByRaisedByCommand_OrderByRaisedAtDesc(raisedBy: CommandId): List<StoredEvent> {
        return findAll().filter { it.raisedByCommand == raisedBy }.sortedByDescending { it.raisedAt }
    }

    override fun findByRaisedByFlow_OrderByRaisedAtDesc(raisedBy: CommandId): List<StoredEvent> {
        return findAll().filter { it.raisedByFlow == raisedBy }.sortedByDescending { it.raisedAt }
    }

    override fun findAll_OrderByRaisedAtDesc(ids: MutableIterable<EventId>): List<StoredEvent> {
        return findAll().filter { ids.contains(it.id) }.sortedByDescending { it.raisedAt }
    }

}
