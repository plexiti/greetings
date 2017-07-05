package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.application.*
import com.plexiti.commons.application.CommandStore
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Name
import com.plexiti.utils.scanPackageForClassNames
import com.plexiti.utils.scanPackageForNamedClasses
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
class CommandStore : CommandStore<Command>, ApplicationContextAware, RouteBuilder() {

    init { init() }

    private fun init() {
        types = scanPackageForNamedClasses("com.plexiti", Command::class)
        names = scanPackageForClassNames("com.plexiti", Command::class)
    }

    @Value("\${com.plexiti.app.context}")
    private var context = Name.context

    @Autowired
    private var delegate: StoredCommandStore = InMemoryStoredCommandStore()

    lateinit internal var types: Map<Name, KClass<out Command>>
    lateinit internal var names: Map<KClass<out Command>, Name>

    internal fun type(qName: Name): KClass<out Command> {
        return types.get(qName) ?: throw IllegalArgumentException("Command type '${qName.qualified}' is not mapped to a local object type!")
    }

    internal fun toCommand(stored: StoredCommand?): Command? {
        return if (stored != null) Command.fromJson(stored.json, type(stored.name)) else null
    }

    internal fun toEntity(command: Command?): StoredCommand? {
        return if (command != null) (delegate.findOne(command.id) ?: StoredCommand(command)) else null
    }

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Command.store = this
        Name.context = context; init()
    }

    override fun configure() {
        types.entries.forEach {
            if (it.key.context == Name.context) {
                val commandName = it.key.name
                val methodName = commandName.substring(0, 1).toLowerCase() + commandName.substring(1)
                val className = it.value.qualifiedName!!
                try {
                    val bean = Class.forName(className.substring(0, className.length - methodName.length - 1))
                    bean.getMethod(methodName, it.value.java)
                    from("direct:${commandName}")
                        .bean(bean, methodName)
                } catch (n: NoSuchMethodException) {}
                  catch (c: ClassNotFoundException) {}
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

    override fun findFirstByNameAndFlowIdOrderByIssuedAtDesc(name: Name, flowId: CommandId): Command?{
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
internal interface StoredCommandStore : CommandStore<StoredCommand>

@NoRepositoryBean
class InMemoryStoredCommandStore : InMemoryEntityCrudRepository<StoredCommand, CommandId>(), StoredCommandStore {

    override fun findByCorrelationAndExecutionFinishedAtIsNull(correlation: Correlation): StoredCommand? {
        return findAll().find { correlation == it.correlation && it.execution.finishedAt == null }
    }

    override fun findFirstByNameAndFlowIdOrderByIssuedAtDesc(name: Name, flowId: CommandId): StoredCommand? {
        return findAll().sortedByDescending { it.issuedAt }.first { it.name == name && it.flowId == flowId }
    }

}
