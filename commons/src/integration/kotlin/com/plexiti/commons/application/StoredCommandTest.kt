package com.plexiti.commons.application

import com.plexiti.commons.DataJpaIntegration
import com.plexiti.commons.adapters.db.StoredEventStore
import com.plexiti.commons.adapters.db.StoredCommandStore
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Name
import com.plexiti.commons.domain.StoredEvent
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*

import org.junit.Test

import org.springframework.beans.factory.annotation.Autowired

/**
 * @author Martin Schimak <martin.schimak></martin.schimak>@plexiti.com>
 */
class StoredCommandTest : DataJpaIntegration() {

    @Autowired
    lateinit var commandStore: StoredCommandStore

    @Autowired
    lateinit var eventStore: StoredEventStore

    class SomeCommand: Command()
    class SomeEvent: Event()

    @Test
    fun save() {

        val event = StoredEvent(SomeEvent())
        eventStore.save(event)

        val command = StoredCommand(SomeCommand())
        commandStore.save(command)

    }

    @Test
    fun EventStore_findFirstByName_OrderByRaisedAtDesc() {

        val event = StoredEvent(SomeEvent())
        eventStore.save(event)

        val result = eventStore.findFirstByName_OrderByRaisedAtDesc(event.name, mutableListOf(event.id))
        assertThat(result).isNotNull().isNotEmpty().first().hasFieldOrPropertyWithValue("id", event.id)

    }

    @Test
    fun EventStore_findFirstByName_OrderByRaisedAtDesc_MultipleIds() {

        val event1 = StoredEvent(SomeEvent())
        eventStore.save(event1)

        val event2 = StoredEvent(SomeEvent())
        eventStore.save(event2)

        val result = eventStore.findFirstByName_OrderByRaisedAtDesc(SomeEvent().name, mutableListOf(event1.id, event2.id))
        assertThat(result).isNotNull().isNotEmpty().first().hasFieldOrPropertyWithValue("id", event2.id)

    }

    @Test
    fun CommandStore_findFirstByName_AndIssuedBy_OrderByIssuedAtDesc() {

        val command = StoredCommand(SomeCommand())
        command.issuedBy = CommandId("issuedBy")
        commandStore.save(command)

        val result = commandStore.findFirstByName_AndIssuedBy_OrderByIssuedAtDesc(command.name, command.issuedBy!!)
        assertThat(result).isNotNull()

    }

    @Test
    fun CommandStore_findFirstByName_AndIssuedBy_OrderByIssuedAtDesc_WrongName() {

        val command = StoredCommand(SomeCommand())
        command.issuedBy = CommandId("issuedBy")
        commandStore.save(command)

        val result = commandStore.findFirstByName_AndIssuedBy_OrderByIssuedAtDesc(Name("Some_Name"), command.issuedBy!!)
        assertThat(result).isNull()

    }

    @Test
    fun CommandStore_findFirstByName_AndIssuedBy_OrderByIssuedAtDesc_WrongIssuedBy() {

        val command = StoredCommand(SomeCommand())
        command.issuedBy = CommandId("issuedBy")
        commandStore.save(command)

        val result = commandStore.findFirstByName_AndIssuedBy_OrderByIssuedAtDesc(command.name, CommandId("someId"))
        assertThat(result).isNull()

    }

}
