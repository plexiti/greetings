package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.application.*
import com.plexiti.commons.application.CommandStore
import com.plexiti.commons.domain.EventId
import com.plexiti.commons.domain.Name
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@NoRepositoryBean
class CommandStore : CommandStore<Command> {

    @Autowired
    private var delegate: StoredCommandStore = InMemoryStoredCommandStore()

    internal fun toCommand(stored: StoredCommand?): Command? {
        return if (stored != null) Command.fromJson(stored.json, Command.type(stored.name)) else null
    }

    internal fun toEntity(command: Command?): StoredCommand? {
        return if (command != null) delegate.findOne(command.id)
            ?: if (command is Flow) StoredFlow(command) else StoredCommand(command) else null
    }

    override fun exists(id: CommandId?): Boolean {
        return delegate.exists(id)
    }

    override fun findOne(id: CommandId?): Command? {
        return toCommand(delegate.findOne(id))
    }

    fun commandId(json: String): CommandId? {
        try {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val id =  node.get("id").textValue()
            return if (id != null) CommandId(id) else null
        } catch (ex: JsonMappingException) {
            return null
        }
    }

    fun findOne(json: String): Command? {
        return findOne(commandId(json))
    }

    override fun findByCorrelatedBy_AndExecutionFinishedAt_IsNull(correlation: Correlation): Command? {
        return toCommand(delegate.findByCorrelatedBy_AndExecutionFinishedAt_IsNull(correlation))
    }

    override fun findFirstByName_AndIssuedBy_OrderByIssuedAtDesc(name: Name, issuedBy: CommandId): Command?{
        return toCommand(delegate.findFirstByName_AndIssuedBy_OrderByIssuedAtDesc(name, issuedBy))
    }

    override fun findByEventsAssociated_Containing(eventId:String): List<Command> {
        return delegate.findByEventsAssociated_Containing(eventId).mapTo (ArrayList(), { toCommand(it)!! })
    }

    override fun findAll(): MutableIterable<Command> {
        return delegate.findAll().mapTo(ArrayList(), { toCommand(it)!! })
    }

    override fun findAll(ids: MutableIterable<CommandId>?): MutableIterable<Command> {
        return delegate.findAll(ids).mapTo(ArrayList(), { toCommand(it)!! })
    }

    override fun <S : Command?> save(command: S): S {
        @Suppress("unchecked_cast")
        return toCommand(delegate.save(toEntity(command))) as S
    }

    override fun <S : Command?> save(commands: MutableIterable<S>?): MutableIterable<S> {
        return commands?.mapTo(ArrayList(), { save(it) })!!
    }

    override fun count(): Long {
        return delegate.count()
    }

    override fun delete(entities: MutableIterable<Command>?) {
        delegate.delete(entities?.map { toEntity(it) })
    }

    override fun delete(command: Command?) {
        delegate.delete(toEntity(command))
    }

    override fun delete(id: CommandId?) {
        delegate.delete(id)
    }

    override fun deleteAll() {
        delegate.deleteAll()
    }

}

@Repository
interface StoredCommandStore : CommandStore<StoredCommand>

@NoRepositoryBean
class InMemoryStoredCommandStore : InMemoryEntityCrudRepository<StoredCommand, CommandId>(), StoredCommandStore {

    override fun findByCorrelatedBy_AndExecutionFinishedAt_IsNull(correlation: Correlation): StoredCommand? {
        return findAll().find { correlation == it.correlatedBy && it.execution?.finishedAt == null }
    }

    override fun findFirstByName_AndIssuedBy_OrderByIssuedAtDesc(name: Name, issuedBy: CommandId): StoredCommand? {
        return findAll().sortedByDescending { it.issuedAt }.first { it.name == name && it.issuedBy == issuedBy }
    }

    override fun findByEventsAssociated_Containing(eventId: String): List<StoredCommand> {
        return findAll().filter { it.eventsAssociated?.contains(EventId(eventId)) ?: false }
    }

}
