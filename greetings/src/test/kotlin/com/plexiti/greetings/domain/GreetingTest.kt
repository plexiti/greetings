package com.plexiti.greetings.domain

import com.plexiti.commons.domain.Event
import org.assertj.core.api.Assertions.*
import org.junit.Test

class GreetingTest {

	@Test
	fun testGreetingCreated() {

        val greeting = Greeting.create("Martin");

        val greetingCreatedEvent = Event.findByAggregate(greeting)[0]
        assertThat(greetingCreatedEvent).returns("greetingCreated", { it.name })

	}

    @Test
    fun testGreetingIdentified() {

        val greeting = Greeting.create("Martin");
        greeting.contact()

        assertThat(greeting)
            .returns(1, { it.contacts })
        assertThat(Event.findByAggregate(greeting))
            .hasSize(2)

    }

}
