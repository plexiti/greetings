package com.plexiti.commons.application

import com.plexiti.commons.domain.AbstractMessageEntity
import com.plexiti.commons.domain.MessageId
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
@Entity @Inheritance
@Table(name="COMMANDS")
@DiscriminatorColumn(name="type", columnDefinition = "varchar(128)", discriminatorType = DiscriminatorType.STRING)
open class Command: AbstractMessageEntity<CommandId>() {

    @Column(name = "ISSUED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    val issuedAt = Date()

    @Column(name = "ISSUED_BY")
    lateinit var issuedBy: String; private set

    companion object {

        lateinit var commandRepository: CommandRepository
        private var command: ThreadLocal<Command?> = ThreadLocal()

        fun <D: Command> issue(command: D): D {
            command.id = CommandId(UUID.randomUUID().toString())
            commandRepository.save(command)
            this.command.set(command)
            return command
        }

        fun active(): Command? {
            return this.command.get()
        }

    }

}

class CommandId(value: String = ""): MessageId(value)

@Repository
interface CommandRepository: CrudRepository<Command, CommandId>

@Component
private class CommandIssuerInitialiser : ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Command.commandRepository = applicationContext!!.getBean(CommandRepository::class.java)
    }

}
