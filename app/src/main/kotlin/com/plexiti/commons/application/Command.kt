package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.adapters.db.InMemoryEntityCrudRepository
import com.plexiti.commons.domain.*
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
import kotlin.reflect.full.declaredMemberFunctions

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity
@Table(name="COMMANDS")
open class CommandEntity() : AbstractMessageEntity<Command, CommandId>() {

    @Column(name = "ISSUED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    lateinit var issuedAt: Date
        private set

    @Column(name = "ISSUED_BY")
    var issuedBy: String? = null
        private set

    @Column(name="TARGET", columnDefinition = "varchar(64)")
    lateinit var target: String
        protected set

    internal constructor(command: Command): this() {
        this.message = command.message
        this.origin = command.origin
        this.id = CommandId(command.id)
        this.type = command.type
        this.definition = command.definition
        this.issuedAt = command.issuedAt
        this.issuedBy = command.issuedBy
        this.target = command.target
        this.json = command.json
    }

    fun toCommand(): Command {
        return Command.toCommand(json)
    }

    fun <C: Command> toCommand(type: Class<C>): C {
        return Command.toCommand(json, type)
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
open class Command(issuedBy: String? = null): Message {

    override var message = MessageType.Command
    override val id = UUID.randomUUID().toString()
    override var origin: String? = null
    override val type =
        this::class.java.simpleName.substring(0,1).toLowerCase() +
        this::class.java.simpleName.substring(1)
    override val definition = 0
    val issuedAt = Date()
    var issuedBy = issuedBy
    open val target = context

    @JsonIgnore var json: String = ""
        @JsonIgnore get() {
            if (field == "")
                field = Command.toJson(this)
            return field
        }
        @JsonIgnore internal set

    companion object {

        private val logger = LoggerFactory.getLogger(this::class.java)

        lateinit var context: String

        var repository: CrudRepository<CommandEntity, CommandId> = InMemoryCommandRepository()
            internal set
        private var active: ThreadLocal<Command?> = ThreadLocal()

        fun <C: Command> issue(command: C): C {
            command.origin = context
            repository.save(CommandEntity(command))
            active.set(command)
            logger.info("Command issued ${command.json}")
            return command
        }

        internal fun toCommand(json: String): Command {
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
    }

}

open class CommandExecutor : RouteBuilder() {

    override fun configure() {

        this::class.declaredMemberFunctions.forEach {
            from("direct:${it.name}").bean(this::class.java, it.name)
        }

    }

}
