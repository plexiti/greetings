package com.plexiti.commons.application

import com.plexiti.commons.domain.AbstractMessage
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
open class Command(id: CommandId? = null): AbstractMessage<CommandId>(id) {

    @Column(name = "ISSUED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    lateinit var issuedAt: Date; private set

    @Column(name = "ISSUED_BY")
    lateinit var issuedBy: String; private set

    init {
        if (id != null) {
            issuedAt = Date()
            Commands.issue(this)
        }
    }

}

class CommandId(value: String? = null): MessageId(value)

@Repository
interface CommandRepository: CrudRepository<Command, CommandId>

private object Commands {

    lateinit var commandRepository: CommandRepository

    fun issue(command: Command) {
        commandRepository.save(command)
    }

}

@Component
private class CommandIssuerInitialiser : ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Commands.commandRepository = applicationContext!!.getBean(CommandRepository::class.java)
    }

}
