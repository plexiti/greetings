package com.plexiti.commons.adapters.db

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

    class ITAggregate : Aggregate<AggregateId>()
    class ITAggregateId(value: String = ""): AggregateId(value)
    class ITEvent(aggregate: ITAggregate? = null): Event(aggregate)

    lateinit var aggregate: ITAggregate

    @Before
    fun prepare() {
        aggregate = ITAggregate()
        aggregate.id = ITAggregateId(UUID.randomUUID().toString())
    }

    @Test
    fun empty () {
        assertThat(eventStore.findAll()).isEmpty()
    }

    @Test
    fun raise() {
        Event.raise(ITEvent(aggregate))
    }

    @Test
    fun find() {
        val event = Event.raise(ITEvent(aggregate))
        val e = eventStore.findOne(event.id)
        assertThat(e)
            .isEqualTo(event)
    }

    @Test
    fun findOne_Json() {
        val expected = Event.raise(ITEvent(aggregate))
        val actual = eventStore.findOne(expected.toJson())
        assertThat(actual).isEqualTo(expected)
    }

}
