package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.JsonMappingException
import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandEntity
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.application.CommandRepository
import com.plexiti.commons.domain.Context
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
class CommandStore: CommandRepository<Command>, ApplicationContextAware {

    @Value("\${com.plexiti.app.context}")
    private var context: String? = null

    @Autowired
    private var delegate: CommandEntityRepository = InMemoryCommandEntityRepository()

    private class RawCommand: Command()

    internal lateinit var commandTypes: Map<String, KClass<out Command>>

    internal fun type(qName: String): KClass<out Command> {
        return commandTypes.get(qName) ?: throw IllegalArgumentException("Command type '$qName' is not mapped to a local object type!")
    }

    private fun toCommand(entity: CommandEntity?): Command? {
        if (entity != null) {
            val command = Command.fromJson(entity.json, type(entity.qname()))
            command.entity = entity
            return command
        }
        return null
    }

    private fun toEntity(command: Command?): CommandEntity? {
        return if (command != null) (delegate.findOne(command.id) ?: CommandEntity(command)) else null
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        Context.home = Context(context)
        Command.store = this
        commandTypes = scanPackageForAssignableClasses("com.plexiti", Command::class.java)
            .map { it.newInstance() as Command }
            .associate { Pair(it.qname(), it::class) }
    }

    override fun exists(id: CommandId?): Boolean {
        return delegate.exists(id)
    }

    override fun findOne(id: CommandId?): Command? {
        return toCommand(delegate.findOne(id))
    }

    fun commandId(json: String): CommandId? {
        try {
            return Command.fromJson(json, RawCommand::class).id
        } catch (ex: JsonMappingException) {
            return null
        }
    }

    fun findOne(json: String): Command? {
        return findOne(commandId(json))
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
internal interface CommandEntityRepository: CommandRepository<CommandEntity>
