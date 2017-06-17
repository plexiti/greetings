package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.adapters.db.InMemoryEntityCrudRepository
import com.plexiti.commons.domain.*
import org.apache.camel.Handler
import org.apache.camel.ProducerTemplate
import org.apache.camel.builder.RouteBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity
@Table(name="COMMANDS")
@NamedQueries(
    NamedQuery(name = "CommandQueuer", query = "select c from CommandEntity c " +
        "where c.async = true and c.publishedAt is null"),
    NamedQuery(name = "CommandFinisher", query = "select c from CommandEntity c " +
        "where c.flowId is not null " +
        "and c.completedBy is not null " +
        "and c.completedAt is null")
)
class CommandEntity() : AbstractMessageEntity<Command, CommandId>() {

    @Column(name = "ISSUED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    lateinit var issuedAt: Date
        private set

    @Column(name = "TRIGGERED_BY", length=36)
    var triggeredBy: String? = null
        private set

    @Column(name = "FLOW_ID", length=36)
    var flowId: String? = null
        private set

    @Column(name = "CORRELATION_ID", length=128)
    lateinit var correlationId: String
        private set

    @Column(name = "COMPLETED_BY", length=36)
    var completedBy: String? = null
        internal set

    @Column(name = "COMPLETED_AT")
    var completedAt: Date? = null
        internal set

    @Column(name="ASYNC")
    var async = false
        internal set

    @Column(name="TARGET", columnDefinition = "varchar(64)")
    lateinit var target: String
        protected set

    internal constructor(command: Command): this() {
        this.type = command.type
        this.origin = command.origin
        this.id = CommandId(command.id)
        this.name = command.name
        this.definition = command.definition
        this.issuedAt = command.issuedAt
        this.triggeredBy = command.triggeredBy
        this.correlationId = command.correlationId
        this.target = command.target
        this.flowId = command.flowId
        this.json = command.json
    }

    fun toCommand(): Command {
        return Command.toCommand(json)
    }

    fun <C: Command> toCommand(type: Class<C>): C {
        return Command.toCommand(json, type)
    }

    override fun setConsumed() {
        if (publishedAt == null) {
            publishedAt = Date()
        } else if (completedAt == null) {
            completedAt = Date()
        }
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
open class Command(triggeredBy: String? = null): Message {

    override var type = MessageType.Command
    override var name =
        this::class.java.simpleName.substring(0,1).toLowerCase() +
            this::class.java.simpleName.substring(1)
    override var origin: String = context
    open val target = context
    override val id = UUID.randomUUID().toString()
    override val definition = 0
    val issuedAt = Date()

    var triggeredBy = triggeredBy
    var correlationId = id
    var flowId: String? = null

    @JsonIgnore var json: String = ""
        @JsonIgnore get() {
            if (field == "")
                field = Command.toJson(this)
            return field
        }
        @JsonIgnore internal set

    open fun isTriggeredBy(event: Event): Boolean {
        return false
    }

    open fun async() {
        val entity = CommandEntity(this)
        entity.async = true
        repository.save(entity)
    }

    open fun sync(): Any? {
        repository.save(CommandEntity(this))
        return execute()
    }

    open internal fun execute(): Any? {
        logger.info("Started ${json}")
        active.set(this)
        val result = router?.requestBody("direct:${name}", this)
        return result
    }

    open fun isCorrelatedWith(event: Event): String? {
        return event.commandId
    }

    open internal fun correlate(event: Event) {
        val commandEntity = repository.findOne(CommandId(id))
        commandEntity.completedBy = event.id
        if (flowId == null) {
            commandEntity.completedAt = Date()
        }
        logger.info("Executed ${json}")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(this::class.java)

        lateinit var context: String

        var repository: CommandRepository = InMemoryCommandRepository()
            internal set

        var router: ProducerTemplate? = null
            internal set

        private var active: ThreadLocal<Command?> = ThreadLocal()

        internal fun toCommand(json: String): Command{
            val command = toCommand(json, Command::class.java)
            command.json = json
            return command
        }

        internal fun <C: Command> toCommand(json: String, type: Class<C>): C {
            val command = ObjectMapper().readValue(json, type)
            command.json = json
            return command
        }

        internal fun toJson(command: Command): String {
            return ObjectMapper().writeValueAsString(command)
        }

        fun active(): Command? {
            return this.active.get()
        }

        val commandTypes = mutableSetOf<Class<Command>>()

        internal fun register(type: Class<Command>) {
            commandTypes.add(type)
        }

        internal fun triggerBy(event: Event): List<Command> {
            val commands = mutableListOf<Command>()
            commandTypes.forEach {
                val command = it.newInstance()
                if (command.isTriggeredBy(event)) {
                    command.triggeredBy = event.id
                    commands.add(command)
                }
            }
            return commands
        }

        internal fun correlateBy(event: Event): Command? {
            commandTypes.forEach {
                var command = it.newInstance()
                val correlationId = command.isCorrelatedWith(event)
                if (correlationId != null) {
                    command = Command.findByCorrelationIdAndCompletedByIsNull(correlationId)
                    return command
                }
            }
            return null
        }

        fun findOne(id: String): Command? {
            return Command.repository.findOne(CommandId(id))?.toCommand()
        }

        fun findByCorrelationIdAndCompletedByIsNull(correlationId: String): Command? {
            return Command.repository.findByCorrelationIdAndCompletedByIsNull(correlationId)?.toCommand()
        }

    }

}

class CommandId(value: String = ""): MessageId(value)

@Repository
interface CommandRepository: CrudRepository<CommandEntity, CommandId> {

    fun findByCorrelationIdAndCompletedByIsNull(correlationId: String): CommandEntity?

}

@NoRepositoryBean
class InMemoryCommandRepository: InMemoryEntityCrudRepository<CommandEntity, CommandId>(), CommandRepository {

    override fun findByCorrelationIdAndCompletedByIsNull(correlationId: String): CommandEntity? {
        val list = findAll().filter { correlationId == it.correlationId && it.completedBy == null }
        return if (list.isNotEmpty()) list[0] else null
    }

}

@Component
private class CommandInitialiser : ApplicationContextAware {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Command.context = context
        Command.repository = applicationContext!!.getBean(CommandRepository::class.java)
        Command.router = applicationContext.getBean(ProducerTemplate::class.java)
    }

}

open class CommandExecutor : RouteBuilder() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun configure() {

        this::class.java.declaredMethods.forEach {
           if (it.parameterTypes.isNotEmpty()) {
               val type = it.parameterTypes[0]
               if (Command::class.java.isAssignableFrom(type)) {
                   from("direct:${it.name}").bean(object {
                       @Handler
                       fun handle(c: Command): Command {
                           return Command.toCommand(c.json, type as Class<Command>)
                       }
                   }).bean(this::class.java, it.name)
                   Command.register(type as Class<Command>)
               }
           }
        }

    }

}
