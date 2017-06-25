package com.plexiti.commons.application

import com.plexiti.commons.AbstractDataJpaTest
import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
open class CorrelationServiceIT : AbstractDataJpaTest() {

    @Autowired
    lateinit var correlationService: CorrelationService

    lateinit var aggregate: ITAggregate

    class InternalITEvent(aggregate: ITAggregate? = null) : Event(aggregate)
    class ExternalITEvent(aggregate: ITAggregate? = null) : Event(aggregate) {
        override var context = Context("External")
    }
    class ITAggregate: Aggregate<AggregateId>()
    class ITAggregateId(value: String = ""): AggregateId(value)

    @Before
    fun prepare() {
        aggregate = ITAggregate()
        aggregate.id = ITAggregateId(UUID.randomUUID().toString())
    }

    @Test
    fun consume_external() {

        val external = ExternalITEvent(aggregate)
        correlationService.handleEvent(external.toJson())
        val event = Event.store.findAll().iterator().next()

        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNull()
        assertThat(event.internals.consumedAt).isNotNull()

    }

    @Test
    fun consume_internal() {

        Event.raise(InternalITEvent(aggregate))
        val event = Event.store.findAll().iterator().next()
        event.internals.transitioned()

        correlationService.handleEvent(event.toJson())

        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNotNull()
        assertThat(event.internals.consumedAt).isNotNull()

    }

}
