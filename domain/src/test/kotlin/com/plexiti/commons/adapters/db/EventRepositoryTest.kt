package com.plexiti.commons.adapters.db

import com.plexiti.commons.domain.Name
import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class EventRepositoryTest {

    class TestAggregate: Aggregate<AggregateId>()
    class TestAggregateId(value: String = ""): AggregateId(value)
    class TestEvent(aggregate: TestAggregate? = null): Event(aggregate)

    lateinit var aggregate: TestAggregate

    @Before
    fun prepare() {
        aggregate = TestAggregate()
        aggregate.id = TestAggregateId(UUID.randomUUID().toString())
        Event.store.eventTypes = mapOf("${Name.default.context}/${TestEvent::class.simpleName}" to TestEvent::class)
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
    fun findOne() {
        val event = Event.raise(TestEvent(aggregate))
        val e = Event.store.findOne(event.id)
        assertThat(e)
            .isEqualTo(event)
    }

    @Test
    fun findOne_Null() {
        val e = Event.store.findOne(EventId("anId"))
        assertThat(e).isNull()
    }

    @Test
    fun findOne_Json() {
        val expected = Event.raise(TestEvent(aggregate))
        val actual = Event.store.findOne(expected.toJson())
        assertThat(actual).isEqualTo(expected)
    }

}
