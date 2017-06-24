package com.plexiti.commons.adapters.db

import com.plexiti.commons.domain.Aggregate
import com.plexiti.commons.domain.AggregateId
import com.plexiti.commons.domain.Event
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class EventStoreTest {

    class TestAggregate: Aggregate<AggregateId>()
    class TestAggregateId(value: String = ""): AggregateId(value)
    class TestEvent(aggregate: TestAggregate? = null): Event(aggregate)

    lateinit var aggregate: TestAggregate

    @Before
    fun prepare() {
        aggregate = TestAggregate()
        aggregate.id = TestAggregateId(UUID.randomUUID().toString())
        Event.store.eventTypes = mapOf("Commons/TestEvent" to TestEvent::class.java)
        Event.store.deleteAll()
    }

    @Test
    fun empty () {
        assertThat(Event.store.findAll()).isEmpty()
    }

    @Test
    fun save() {
        Event.raise(TestEvent(aggregate))
    }

    @Test
    fun find() {
        val event = Event.raise(TestEvent(aggregate))
        val e = Event.store.findOne(event.id)
        assertThat(e)
            .isEqualTo(event)
    }

}
