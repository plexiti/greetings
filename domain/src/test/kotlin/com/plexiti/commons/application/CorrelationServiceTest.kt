package com.plexiti.commons.application

import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class CorrelationServiceTest {

    val correlationService: CorrelationService = CorrelationService()
    lateinit var aggregate: CorrelationServiceTest.TestAggregate

    class InternalEvent(aggregate: TestAggregate? = null) : Event(aggregate)
    class ExternalEvent(aggregate: TestAggregate? = null) : Event(aggregate) {
        override var context = Context("External")
    }
    class TestAggregate: Aggregate<AggregateId>()
    class TestAggregateId(value: String = ""): AggregateId(value)

    class TriggeredTestCommand(): Command() {
        override fun triggerBy(event: Event): Command? {
            return if (event.qname().equals("External/ExternalEvent")) TriggeredTestCommand() else null
        }
    }

    @Before
    fun prepare() {
        aggregate = TestAggregate()
        aggregate.id = TestAggregateId(UUID.randomUUID().toString())
        Event.store.eventTypes = mapOf(
            "${Context.home.name}/${InternalEvent::class.simpleName}" to InternalEvent::class,
            "External/${ExternalEvent::class.simpleName}" to ExternalEvent::class
        )
        Command.store.commandTypes = mapOf(
            "${Context.home.name}/${TriggeredTestCommand::class.simpleName}" to TriggeredTestCommand::class
        )
        Event.store.deleteAll()
        Command.store.deleteAll()
        correlationService.eventStore = Event.store
        correlationService.commandStore = Command.store
    }

    @Test
    fun consumeEvent_External() {

        val external = ExternalEvent(aggregate)
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
    fun consumeEvent_Internal() {

        Event.raise(InternalEvent(aggregate))
        val event = Event.store.findAll().iterator().next()
        event.internals.transitioned()

        correlationService.consumeEvent(event.toJson())
        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNotNull()
        assertThat(event.internals.consumedAt).isNotNull()

        assertThat(Command.store.findAll()).isEmpty()

    }

}
