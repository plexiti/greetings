package com.plexiti.commons.adapters.db

import com.plexiti.commons.application.Application
import com.plexiti.commons.application.Command
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class CommandStoreTest {

    var application = Application()

    class TestCommand(): Command()

    @Before
    fun prepare() {
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

    @Test
    fun findOne_Json() {
        val expected = Command.issue(TestCommand())
        val actual = Command.store.findOne(expected.toJson())
        assertThat(actual).isEqualTo(expected)
    }

}
