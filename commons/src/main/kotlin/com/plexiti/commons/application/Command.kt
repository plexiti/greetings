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
open class CommandEntity() : AbstractMessageEntity<Command<*>, CommandId>() {

    @Column(name = "ISSUED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    lateinit var issuedAt: Date
        private set

    @Column(name = "TRIGGERED_BY")
    var triggeredBy: String? = null
        private set

    @Column(name="TARGET", columnDefinition = "varchar(64)")
    lateinit var target: String
        protected set

    internal constructor(command: Command<*>): this() {
        this.type = command.type
        this.origin = command.origin
        this.id = CommandId(command.id)
        this.name = command.name
        this.definition = command.definition
        this.issuedAt = command.issuedAt
        this.triggeredBy = command.triggeredBy
        this.target = command.target
        this.json = command.json
    }

    fun toCommand(): Command<*> {
        return Command.toCommand(json)
    }

    fun <C: Command<*>> toCommand(type: Class<C>): C {
        return Command.toCommand(json, type)
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
open class Command<R: Any?>(triggeredBy: String? = null): Message {

    override var type = MessageType.Command
    override val name =
        this::class.java.simpleName.substring(0,1).toLowerCase() +
            this::class.java.simpleName.substring(1)
    override var origin: String? = null
    override val id = UUID.randomUUID().toString()
    override val definition = 0
    val issuedAt = Date()
    var triggeredBy = triggeredBy
    open val target = context

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

    companion object {

        private val logger = LoggerFactory.getLogger(this::class.java)

        lateinit var context: String

        var repository: CrudRepository<CommandEntity, CommandId> = InMemoryCommandRepository()
            internal set

        var router: ProducerTemplate? = null
            internal set

        private var active: ThreadLocal<Command<Any?>?> = ThreadLocal()

        fun <R, C: Command<R>> issue(command: C): R {
            command.origin = context
            repository.save(CommandEntity(command))
            active.set(command as Command<Any?>)
            logger.info("Issued ${command.json}")
            val result = router?.requestBody("direct:${command.name}", command)
            logger.info("Executed ${command.json}")
            return result as R
        }

        internal fun toCommand(json: String): Command<Any?> {
            val command = toCommand(json, Command::class.java)
            command.json = json
            return command as Command<Any?>
        }

        internal fun <C: Command<*>> toCommand(json: String, type: Class<C>): C {
            val command = ObjectMapper().readValue(json, type)
            command.json = json
            return command
        }

        internal fun toJson(command: Command<*>): String {
            return ObjectMapper().writeValueAsString(command)
        }

        fun active(): Command<Any?>? {
            return this.active.get()
        }

        val commandTypes = mutableSetOf<Class<Command<Any?>>>()

        internal fun register(type: Class<Command<Any?>>) {
            commandTypes.add(type)
        }

        internal fun triggerBy(event: Event): List<Command<Any?>> {
            val commands = mutableListOf<Command<Any?>>()
            commandTypes.forEach {
                val command = it.newInstance()
                if (command.isTriggeredBy(event)) {
                    command.triggeredBy = event.id
                    commands.add(command)
                }
            }
            return commands
        }

    }

}

class CommandId(value: String = ""): MessageId(value)

@Repository
interface CommandRepository: CrudRepository<CommandEntity, CommandId>

@NoRepositoryBean
class InMemoryCommandRepository: InMemoryEntityCrudRepository<CommandEntity, CommandId>(), CommandRepository

@Component
private class CommandIssuerInitialiser : ApplicationContextAware {

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
            val command = if (it.parameterTypes.isNotEmpty()
                && Command::class.java.isAssignableFrom(it.parameterTypes[0]))
                it.parameterTypes[0] else null
            if (command != null) {
                from("direct:${it.name}").bean(object {
                    @Handler
                    fun handle(c: Command<Any?>): Command<Any?> {
                        return Command.toCommand(c.json, command as Class<Command<Any?>>)
                    }
                }).bean(this::class.java, it.name)
                Command.register(command as Class<Command<Any?>>)
            }
        }

    }

}
