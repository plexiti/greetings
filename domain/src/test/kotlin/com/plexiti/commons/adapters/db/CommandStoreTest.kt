package com.plexiti.commons.adapters.db

import com.plexiti.commons.application.Command
import com.plexiti.commons.domain.Aggregate
import com.plexiti.commons.domain.AggregateId
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class CommandStoreTest {

    internal var commandStore = CommandStore()

    class TestCommand(): Command()

    lateinit var command: TestCommand

    @Before
    fun prepare() {
        command = TestCommand()
        commandStore.commandTypes = mapOf("Commons/TestCommand" to TestCommand::class.java)
    }

    @Test
    fun empty () {
        assertThat(commandStore.findAll()).isEmpty()
    }

    @Test
    fun save() {
        commandStore.save(command)
    }

    @Test
    fun find() {
        commandStore.save(command)
        val e = commandStore.findOne(command.id)
        assertThat(e)
            .isEqualTo(command)
    }

}
