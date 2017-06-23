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

    class TestAggregate: Aggregate<AggregateId>()
    class TestAggregateId(value: String = ""): AggregateId(value)
    class TestEvent(aggregate: TestAggregate? = null): Event(aggregate)

    lateinit var event: TestEvent
    lateinit var aggregate: TestAggregate

    @Before
    fun prepare() {
        aggregate = TestAggregate()
        aggregate.id = TestAggregateId(UUID.randomUUID().toString())
        event = TestEvent(aggregate)
    }

    @Test
    fun empty () {
        assertThat(eventStore.findAll()).isEmpty()
    }

    @Test
    fun save() {
        event = TestEvent(aggregate)
        eventStore.save(event)
    }

    @Test
    fun find() {
        event = TestEvent(aggregate)
        eventStore.save(event)
        val e = eventStore.findOne(event.id)
        assertThat(e)
            .isEqualTo(event)
    }

}
