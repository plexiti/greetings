package com.plexiti.commons.application

import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class ApplicationServiceTest {

    val applicationService: ApplicationService = ApplicationService()
    lateinit var aggregate: ApplicationServiceTest.TestAggregate

    class ApplicationInternalEvent(aggregate: TestAggregate? = null) : Event(aggregate)
    class ApplicationExternalEvent(aggregate: TestAggregate? = null) : Event(aggregate) {
        override var name = Name("External_ApplicationExternalEvent")
    }
    class TestAggregate: Aggregate<AggregateId>()
    class TestAggregateId(value: String = ""): AggregateId(value)

    class TriggeredTestCommand(): Command() {
        override fun trigger(event: Event): Command? {
            return if (event.name.qualified.equals("External_ApplicationExternalEvent")) TriggeredTestCommand() else null
        }
    }

    @Before
    fun prepare() {
        aggregate = TestAggregate()
        aggregate.id = TestAggregateId(UUID.randomUUID().toString())
        Event.store.eventTypes = mapOf(
            "${Name.default.context}_${ApplicationInternalEvent::class.simpleName}" to ApplicationInternalEvent::class,
            "External_${ApplicationExternalEvent::class.simpleName}" to ApplicationExternalEvent::class
        )
        Command.store.commandTypes = mapOf(
            "${Name.default.context}_${TriggeredTestCommand::class.simpleName}" to TriggeredTestCommand::class
        )
        Event.store.deleteAll()
        Command.store.deleteAll()
        applicationService.eventRepository = Event.store
        applicationService.commandRepository = Command.store
    }

    @Test
    fun consumeEvent_External() {

        val external = ApplicationExternalEvent(aggregate)
        applicationService.consumeEvent(external.toJson())

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

        Event.raise(ApplicationInternalEvent(aggregate))
        val event = Event.store.findAll().iterator().next()
        event.internals.forward()

        applicationService.consumeEvent(event.toJson())
        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNotNull()
        assertThat(event.internals.consumedAt).isNotNull()

        assertThat(Command.store.findAll()).isEmpty()

    }

}
