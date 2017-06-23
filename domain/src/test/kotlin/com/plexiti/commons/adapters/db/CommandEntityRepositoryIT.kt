package com.plexiti.commons.adapters.db

import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class CommandEntityRepositoryIT : AbstractDataJpaTest() {

    @Autowired
    internal lateinit var commandEntityRepository: CommandEntityRepository

    class TestCommand: Command()

    lateinit var command: TestCommand

    @Before fun prepare() {
        command = TestCommand()
    }

    @Test fun empty () {
        assertThat(commandEntityRepository.findAll()).isEmpty()
    }

    @Test fun save () {
        val command = CommandEntity(TestCommand())
        val e = commandEntityRepository.save(command)
        assertThat(e.isNew()).isFalse()
    }

    @Test fun qualifiedName () {
        val command = CommandEntity(TestCommand())
        assertThat(command.qname()).isEqualTo("Commons/TestCommand")
    }

}
