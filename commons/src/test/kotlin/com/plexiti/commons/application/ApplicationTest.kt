package com.plexiti.commons.application

import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class ApplicationTest {

    val application: Application = Application()
    lateinit var aggregate: ApplicationTest.TestAggregate

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
        Event.store.deleteAll()
        Command.store.deleteAll()
    }

    @Test
    fun consumeEvent_External() {

        val external = ApplicationExternalEvent(aggregate)
        application.consume(external.toJson())

        val event = Event.store.findAll().iterator().next()

        assertThat(event.internals().status).isEqualTo(EventStatus.processed)
        assertThat(event.internals().raisedAt).isNotNull()
        assertThat(event.internals().forwardedAt).isNull()
        assertThat(event.internals().consumedAt).isNotNull()

        val command = Command.store.findAll().iterator().next()
        assertThat(command.internals().status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals().issuedAt).isNotNull()
        assertThat(command.internals().forwardedAt).isNull()
        assertThat(command.internals().getTriggeredBy()).isEqualTo(event.id)

    }

    @Test
    fun consumeEvent_Internal() {

        Event.raise(ApplicationInternalEvent(aggregate))
        val event = Event.store.findAll().iterator().next()
        event.internals().forward()

        application.consume(event.toJson())
        assertThat(event.internals().status).isEqualTo(EventStatus.processed)
        assertThat(event.internals().raisedAt).isNotNull()
        assertThat(event.internals().forwardedAt).isNotNull()
        assertThat(event.internals().consumedAt).isNotNull()

        assertThat(Command.store.findAll()).isEmpty()

    }

}
