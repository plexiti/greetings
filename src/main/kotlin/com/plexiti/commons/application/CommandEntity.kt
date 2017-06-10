package com.plexiti.commons.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.adapters.db.InMemoryEntityCrudRepository
import com.plexiti.commons.domain.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity
@Table(name="COMMANDS")
open class CommandEntity() : AbstractMessageEntity<CommandId>() {

    @Column(name = "ISSUED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    lateinit var issuedAt: Date
        private set

    @Column(name = "ISSUED_BY")
    var issuedBy: String? = null
        private set

    internal constructor(command: Command): this() {
        this.message = command.message
        this.origin = command.origin
        this.id = CommandId(command.id)
        this.type = command.type
        this.definition = command.definition
        this.issuedAt = command.issuedAt
        this.issuedBy = command.issuedBy
        this.json = ObjectMapper().writeValueAsString(command)
    }

}

abstract class Command(issuedBy: String? = null): Message {

    override var message = MessageType.Command
    override val id = UUID.randomUUID().toString()
    override var origin: String? = null
    override val type = this::class.java.simpleName
    val issuedAt = Date()
    var issuedBy = issuedBy

    companion object {

        lateinit var context: String

        var repository: CrudRepository<CommandEntity, CommandId> = InMemoryEntityCrudRepository<CommandEntity, CommandId>()
            internal set
        private var active: ThreadLocal<Command?> = ThreadLocal()

        fun <C: Command> issue(command: C): C {
            command.origin = context
            repository.save(CommandEntity(command))
            active.set(command)
            return command
        }

        fun active(): Command? {
            return this.active.get()
        }

    }

}

class CommandId(value: String = ""): MessageId(value)

@Repository
interface CommandRepository: CrudRepository<CommandEntity, CommandId>

@Component
private class CommandIssuerInitialiser : ApplicationContextAware {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Command.context = context
        Command.repository = applicationContext!!.getBean(CommandRepository::class.java)
    }

}
