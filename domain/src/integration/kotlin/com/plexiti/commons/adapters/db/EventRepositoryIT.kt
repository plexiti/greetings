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
class EventRepositoryIT : com.plexiti.commons.AbstractDataJpaTest() {

    @org.springframework.beans.factory.annotation.Autowired
    internal lateinit var eventRepository: com.plexiti.commons.adapters.db.EventRepository

    class ServiceITAggregate : com.plexiti.commons.domain.Aggregate<AggregateId>()
    class ServiceITAggregateId(value: String = ""): com.plexiti.commons.domain.AggregateId(value)
    class ServiceITEvent(aggregate: com.plexiti.commons.adapters.db.EventRepositoryIT.ServiceITAggregate? = null): com.plexiti.commons.domain.Event(aggregate)

    lateinit var aggregate: com.plexiti.commons.adapters.db.EventRepositoryIT.ServiceITAggregate

    @org.junit.Before
    fun prepare() {
        aggregate = com.plexiti.commons.adapters.db.EventRepositoryIT.ServiceITAggregate()
        aggregate.id = com.plexiti.commons.adapters.db.EventRepositoryIT.ServiceITAggregateId(java.util.UUID.randomUUID().toString())
    }

    @org.junit.Test
    fun empty () {
        assertThat(eventRepository.findAll()).isEmpty()
    }

    @org.junit.Test
    fun raise() {
        com.plexiti.commons.domain.Event.Companion.raise(ServiceITEvent(aggregate))
    }

    @org.junit.Test
    fun find() {
        val event = com.plexiti.commons.domain.Event.Companion.raise(ServiceITEvent(aggregate))
        val e = eventRepository.findOne(event.id)
        assertThat(e)
            .isEqualTo(event)
    }

    @org.junit.Test
    fun findOne_Json() {
        val expected = com.plexiti.commons.domain.Event.Companion.raise(ServiceITEvent(aggregate))
        val actual = eventRepository.findOne(expected.toJson())
        assertThat(actual).isEqualTo(expected)
    }

}
