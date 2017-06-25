package com.plexiti.commons.adapters.db

import com.plexiti.commons.AbstractDataJpaTest
import com.plexiti.commons.application.Command
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class CommandStoreIT: AbstractDataJpaTest() {

    @Autowired
    internal lateinit var commandStore: CommandStore

    class ITCommand(): Command()

    @Test
    fun empty () {
        assertThat(commandStore.findAll()).isEmpty()
    }

    @Test
    fun issue() {
        Command.issue(ITCommand())
    }

    @Test
    fun find() {
        val command = Command.issue(ITCommand())
        val e = commandStore.findOne(command.id)
        assertThat(e)
            .isEqualTo(command)
    }

    @Test
    fun findOne_Json() {
        val expected = Command.issue(ITCommand())
        val actual = commandStore.findOne(expected.toJson())
        assertThat(actual).isEqualTo(expected)
    }

}
