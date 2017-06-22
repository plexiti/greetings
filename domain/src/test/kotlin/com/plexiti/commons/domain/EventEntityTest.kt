package com.plexiti.commons.domain

import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class EventEntityTest: DataJpaTest() {

    class TestAggregate: Aggregate<AggregateId>()
    class TestAggregateId(value: String = ""): AggregateId(value)

    lateinit var aggregate: TestAggregate

    @Before fun prepare() {
        aggregate = TestAggregate()
        aggregate.id = TestAggregateId(UUID.randomUUID().toString())
    }

    @Test fun empty () {
        assertThat(eventRepository.findAll()).isEmpty()
    }

    @Test fun save () {
        val event = EventEntity(aggregate)
        val e = eventRepository.save(event)
        assertThat(e.isNew()).isFalse()
    }

}
