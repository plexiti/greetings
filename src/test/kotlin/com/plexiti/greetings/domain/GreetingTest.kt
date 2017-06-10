package com.plexiti.greetings.domain

import com.plexiti.commons.domain.Event
import org.assertj.core.api.Assertions.*
import org.junit.Test

class GreetingTest {

	@Test
	fun testGreeting() {
        val greeting = Greeting.create("Martin");
        val uuid = greeting.id.value
        assertThat(uuid).isNotNull()
        assertThat(uuid.length).isEqualTo(36)
        assertThat(Event.repository.count() == 1L)
        assertThat(Event.repository.findAll().iterator().next().type == Greeting.GreetingCreated::class.simpleName)
	}

}
