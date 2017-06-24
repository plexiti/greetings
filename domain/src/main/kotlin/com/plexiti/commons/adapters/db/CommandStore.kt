package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandEntity
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.application.CommandRepository
import com.plexiti.commons.domain.*
import com.plexiti.utils.scanPackageForAssignableClasses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component @NoRepositoryBean
class CommandStore: CommandRepository<Command>, ApplicationContextAware {

    @Value("\${com.plexiti.app.context}")
    private var context: String = "Commons"

    @Autowired
    private var delegate: CommandEntityRepository = InMemoryCommandEntityRepository()

    internal lateinit var commandTypes: Map<String, Class<*>>

    private fun toCommand(entity: CommandEntity?): Command? {
        return if (entity != null) ObjectMapper().readValue(entity.json, commandTypes[entity.qname()]) as Command else null
    }

    private fun toEntity(command: Command?): CommandEntity? {
        return if (command != null) (delegate.findOne(command.id) ?: CommandEntity(command)) else null
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        Command.context = Context(context)
        Command.store = this
        commandTypes = scanPackageForAssignableClasses("com.plexiti", Command::class.java)
            .map { it.newInstance() as Command }
            .associate { Pair("${it.context.name}/${it.name}", it::class.java) }
    }

    override fun exists(id: CommandId?): Boolean {
        return delegate.exists(id)
    }

    override fun findOne(id: CommandId?): Command? {
        return toCommand(delegate.findOne(id))
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
