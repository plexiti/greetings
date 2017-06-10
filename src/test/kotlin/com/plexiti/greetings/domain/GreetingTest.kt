package com.plexiti.greetings.domain

import org.assertj.core.api.Assertions.*
import org.junit.Test

class GreetingTest {

	@Test
	fun testGreeting() {
        val greeting = Greeting.create("Martin");
        val uuid = greeting.id.value
        assertThat(uuid).isNotNull()
        assertThat(uuid.length).isEqualTo(36)
	}

}
