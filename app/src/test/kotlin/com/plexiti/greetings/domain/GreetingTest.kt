package com.plexiti.greetings.domain

import com.plexiti.commons.domain.Event
import org.assertj.core.api.Assertions.*
import org.junit.Test

class GreetingTest {

	@Test
	fun testGreetingCreated() {

        val greetingAggregate = Greeting.create("Martin");

        val greetingCreatedEvent = Event.findByAggregate(greetingAggregate)[0]
        assertThat(greetingCreatedEvent).returns("GreetingCreated", { it.type })

	}

}
