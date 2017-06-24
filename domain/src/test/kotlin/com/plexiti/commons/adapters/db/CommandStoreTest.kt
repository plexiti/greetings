package com.plexiti.commons.adapters.db

import com.plexiti.commons.application.Command
import com.sun.deploy.util.SearchPath.findOne
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class CommandStoreTest {

    class TestCommand(): Command()

    @Before
    fun prepare() {
        Command.store.commandTypes = mapOf("Commons/TestCommand" to TestCommand::class.java)
        Command.store.deleteAll()
    }

    @Test
    fun empty () {
        assertThat(Command.store.findAll()).isEmpty()
    }

    @Test
    fun issue() {
        Command.issue(TestCommand())
    }

    @Test
    fun find() {
        val command =  Command.issue(TestCommand())
        val e = Command.store.findOne(command.id)
        assertThat(e)
            .isEqualTo(command)
    }

}
