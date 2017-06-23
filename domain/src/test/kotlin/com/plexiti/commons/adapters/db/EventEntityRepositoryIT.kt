package com.plexiti.commons.adapters.db

import com.plexiti.commons.domain.Aggregate
import com.plexiti.commons.domain.AggregateId
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.EventEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class EventEntityRepositoryIT : AbstractDataJpaTest() {

    @Autowired
    internal lateinit var eventEntityRepository: EventEntityRepository

    class TestAggregate: Aggregate<AggregateId>()
    class TestAggregateId(value: String = ""): AggregateId(value)
    class TestEvent(aggregate: TestAggregate? = null): Event(aggregate)

    lateinit var event: TestEvent
    lateinit var aggregate: TestAggregate

    @Before fun prepare() {
        aggregate = TestAggregate()
        aggregate.id = TestAggregateId(UUID.randomUUID().toString())
        event = TestEvent(aggregate)
    }

    @Test fun empty () {
        assertThat(eventEntityRepository.findAll()).isEmpty()
    }

    @Test fun save () {
        val event = EventEntity(TestEvent(aggregate))
        val e = eventEntityRepository.save(event)
        assertThat(e.isNew()).isFalse()
    }

    @Test fun qualifiedName () {
        val event = EventEntity(TestEvent(aggregate))
        assertThat(event.qname()).isEqualTo("Commons/TestEvent")
    }

}
