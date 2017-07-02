package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.application.*
import com.plexiti.commons.application.CommandRepository
import com.plexiti.commons.domain.Name
import com.plexiti.utils.scanPackageForAssignableClasses
import org.apache.camel.builder.RouteBuilder
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
class CommandRepository : CommandRepository<Command>, ApplicationContextAware, RouteBuilder() {

    @Value("\${com.plexiti.app.context}")
    private var context = Name.default.context

    @Autowired
    private var delegate: CommandEntityRepository = InMemoryCommandEntityRepository()

    internal var commandTypes: Map<String, KClass<out Command>> = emptyMap()

    internal fun type(qName: String): KClass<out Command> {
        return commandTypes.get(qName) ?: throw IllegalArgumentException("Command type '$qName' is not mapped to a local object type!")
    }

    private fun toCommand(entity: CommandEntity?): Command? {
        if (entity != null) {
            val command = Command.fromJson(entity.json, type(entity.name.qualified))
            command.internals = entity
            return command
        }
        return null
    }

    private fun toEntity(command: Command?): CommandEntity? {
        return if (command != null) (delegate.findOne(command.id) ?: CommandEntity(command)) else null
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        Name.default.context = context
        Command.store = this
        commandTypes = scanPackageForAssignableClasses("com.plexiti", Command::class.java)
            .filter { it != Flow::class.java && it != FlowCommand::class.java }
            .map { it.newInstance() as Command }
            .associate { Pair(it.name.qualified, it::class) }
    }

    override fun configure() {
        commandTypes.entries.forEach {
            if (it.key.startsWith(Name.default.context + '/')) {
                val idx = it.key.indexOf('/') + 1
                val commandName = it.key.substring(idx)
                val methodName = it.key.substring(idx, idx + 1).toLowerCase() + it.key.substring(idx + 1)
                val className = it.value.qualifiedName!!
                val bean = Class.forName(className.substring(0, className.length - methodName.length - 1))
                try {
                    bean.getMethod(methodName, it.value.java)
                    from("direct:${commandName}")
                        .bean(bean, methodName)
                } catch (e: NoSuchMethodException) {
                    // fall through
                }
            }
        }
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

    override fun findByCorrelationAndExecutionFinishedAtIsNull(correlation: Correlation): Command? {
        return toCommand(delegate.findByCorrelationAndExecutionFinishedAtIsNull(correlation))
    }

    override fun findFirstByNameAndFlowIdOrderByIssuedAtDesc(name: Name, flowId: FlowId): Command?{
        return toCommand(delegate.findFirstByNameAndFlowIdOrderByIssuedAtDesc(name, flowId))
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

@NoRepositoryBean
class InMemoryCommandEntityRepository: InMemoryEntityCrudRepository<CommandEntity, CommandId>(), CommandEntityRepository {

    override fun findByCorrelationAndExecutionFinishedAtIsNull(correlation: Correlation): CommandEntity? {
        return findAll().find { correlation == it.correlation && it.execution.finishedAt == null }
    }

    override fun findFirstByNameAndFlowIdOrderByIssuedAtDesc(name: Name, flowId: FlowId): CommandEntity? {
        return findAll().sortedByDescending { it.issuedAt }.first { it.name == name && it.flowId == flowId }
    }

}