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

    @Before
    fun prepare() {
        aggregate = TestAggregate()
        aggregate.id = TestAggregateId(UUID.randomUUID().toString())
        Event.store.eventTypes = mapOf(
            "${Context.home.name}/${InternalEvent::class.simpleName}" to InternalEvent::class,
            "External/${ExternalEvent::class.simpleName}" to ExternalEvent::class
        )
        Event.store.deleteAll()
        correlationService.eventStore = Event.store
        correlationService.commandStore = Command.store
    }

    @Test
    fun consume_external() {

        val external = ExternalEvent(aggregate)
        correlationService.handleEvent(external.toJson())
        val event = Event.store.findAll().iterator().next()
        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNull()
        assertThat(event.internals.consumedAt).isNotNull()

    }

    @Test
    fun consume_internal() {

        Event.raise(InternalEvent(aggregate))
        val event = Event.store.findAll().iterator().next()
        event.internals.transitioned()

        correlationService.handleEvent(event.toJson())
        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNotNull()
        assertThat(event.internals.consumedAt).isNotNull()

    }

    @Test
    fun finish() {

        Event.raise(InternalEvent(aggregate))
        val event = Event.store.findAll().iterator().next()
        event.internals.transitioned()

        correlationService.handleEvent(event.toJson())
        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNotNull()
        assertThat(event.internals.consumedAt).isNotNull()

    }

}
