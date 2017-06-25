package com.plexiti.commons.application

import com.plexiti.commons.AbstractDataJpaTest
import com.plexiti.commons.adapters.db.CommandStoreIT
import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
open class CorrelationServiceIT : AbstractDataJpaTest() {

    @Autowired
    lateinit var correlationService: CorrelationService

    lateinit var aggregate: ITAggregate

    class InternalITEvent(aggregate: ITAggregate? = null) : Event(aggregate)
    class ExternalITEvent(aggregate: ITAggregate? = null) : Event(aggregate) {
        override var context = Context("External")
    }
    class ITAggregate: Aggregate<AggregateId>()
    class ITAggregateId(value: String = ""): AggregateId(value)

    class TriggeredITCommand(): Command() {
        override fun triggerBy(event: Event): Command? {
            return if (event.qname().equals("External/ExternalITEvent")) TriggeredITCommand() else null
        }
    }

    @Before
    fun prepare() {
        aggregate = ITAggregate()
        aggregate.id = ITAggregateId(UUID.randomUUID().toString())
    }

    @Test
    fun consumeEvent_external() {

        val external = ExternalITEvent(aggregate)
        correlationService.consumeEvent(external.toJson())

        val event = Event.store.findAll().iterator().next()

        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNull()
        assertThat(event.internals.consumedAt).isNotNull()

        val command = Command.store.findAll().iterator().next()

        assertThat(command.internals.status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals.issuedAt).isNotNull()
        assertThat(command.internals.forwardedAt).isNull()
        assertThat(command.internals.triggeredBy).isEqualTo(event.id)

    }

    @Test
    fun consumeEvent_internal() {

        Event.raise(InternalITEvent(aggregate))
        val event = Event.store.findAll().iterator().next()
        event.internals.transitioned()

        correlationService.consumeEvent(event.toJson())

        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNotNull()
        assertThat(event.internals.consumedAt).isNotNull()

        assertThat(Command.store.findAll()).isEmpty()

    }

    @Test
    fun executeCommand() {

        val external = ExternalITEvent(aggregate)
        correlationService.consumeEvent(external.toJson())

        val event = Event.store.findAll().iterator().next()

        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNull()
        assertThat(event.internals.consumedAt).isNotNull()

        var command = Command.store.findAll().iterator().next()

        assertThat(command.internals.status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals.issuedAt).isNotNull()
        assertThat(command.internals.forwardedAt).isNull()
        assertThat(command.internals.triggeredBy).isEqualTo(event.id)

        command.internals.transitioned()
        correlationService.executeCommand(command.toJson())

        val events = Event.store.findAll()

        assertThat(events).hasSize(2)
        assertThat(events.find { it.name ==  "ExternalITEvent" }).isNotNull()
        assertThat(events.find { it.name ==  "InternalITEvent" }).isNotNull()

        command = Command.store.findAll().iterator().next()

        assertThat(command.internals.status).isEqualTo(CommandStatus.started)
        assertThat(command.internals.startedAt).isNotNull()
        assertThat(command.internals.finishedAt).isNull()

        correlationService.consumeEvent(events.find { it.name ==  "InternalITEvent" }!!.toJson())

        command = Command.store.findAll().iterator().next()

        assertThat(command.internals.status).isEqualTo(CommandStatus.finished)
        assertThat(command.internals.startedAt).isNotNull()
        assertThat(command.internals.finishedAt).isNotNull()

    }


    fun triggeredITCommand(command: TriggeredITCommand) {
        prepare()
        Event.raise(InternalITEvent(aggregate))
    }

}
