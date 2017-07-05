package com.plexiti.commons.adapters.db

import com.plexiti.commons.domain.AggregateId
import org.assertj.core.api.Assertions.assertThat

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class EventStoreIntegration : com.plexiti.commons.DataJpaIntegration() {

    @org.springframework.beans.factory.annotation.Autowired
    internal lateinit var eventRepository: com.plexiti.commons.adapters.db.EventStore

    class ServiceITAggregate : com.plexiti.commons.domain.Aggregate<AggregateId>()
    class ServiceITAggregateId(value: String = ""): com.plexiti.commons.domain.AggregateId(value)
    class ServiceITEvent(aggregate: com.plexiti.commons.adapters.db.EventStoreIntegration.ServiceITAggregate? = null): com.plexiti.commons.domain.Event(aggregate)

    lateinit var aggregate: com.plexiti.commons.adapters.db.EventStoreIntegration.ServiceITAggregate

    @org.junit.Before
    fun prepare() {
        aggregate = com.plexiti.commons.adapters.db.EventStoreIntegration.ServiceITAggregate()
        aggregate.id = com.plexiti.commons.adapters.db.EventStoreIntegration.ServiceITAggregateId(java.util.UUID.randomUUID().toString())
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
