package com.plexiti.greetings.domain

import com.plexiti.commons.domain.Event
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class GreetingTest {

    @Before
    fun init() {
        Event.store.deleteAll()
    }

	@Test
	fun testGreetingCreated() {
        Greeting.create(caller = "Martin");
        assertThat(Event.store.findAll().first())
            .returns("GreetingCreated", { it.name.name })
	}

}
