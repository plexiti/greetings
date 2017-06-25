package com.plexiti.commons.adapters.db

import com.plexiti.commons.AbstractDataJpaTest
import com.plexiti.commons.domain.Aggregate
import com.plexiti.commons.domain.AggregateId
import com.plexiti.commons.domain.Event
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class EventStoreIT: AbstractDataJpaTest() {

    @Autowired
    internal lateinit var eventStore: EventStore

    class ServiceITAggregate : Aggregate<AggregateId>()
    class ServiceITAggregateId(value: String = ""): AggregateId(value)
    class ServiceITEvent(aggregate: ServiceITAggregate? = null): Event(aggregate)

    lateinit var aggregate: ServiceITAggregate

    @Before
    fun prepare() {
        aggregate = ServiceITAggregate()
        aggregate.id = ServiceITAggregateId(UUID.randomUUID().toString())
    }

    @Test
    fun empty () {
        assertThat(eventStore.findAll()).isEmpty()
    }

    @Test
    fun raise() {
        Event.raise(ServiceITEvent(aggregate))
    }

    @Test
    fun find() {
        val event = Event.raise(ServiceITEvent(aggregate))
        val e = eventStore.findOne(event.id)
        assertThat(e)
            .isEqualTo(event)
    }

    @Test
    fun findOne_Json() {
        val expected = Event.raise(ServiceITEvent(aggregate))
        val actual = eventStore.findOne(expected.toJson())
        assertThat(actual).isEqualTo(expected)
    }

}
