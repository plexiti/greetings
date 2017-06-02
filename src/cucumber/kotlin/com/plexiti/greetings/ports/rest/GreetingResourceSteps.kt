package com.plexiti.greetings.ports.rest

import com.plexiti.greetings.domain.Greeting
import cucumber.api.java.en.And
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import org.assertj.core.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@ContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GreetingResourceSteps {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    var caller: String? = null
    var response: ResponseEntity<Greeting>? = null

    @Given("I use the caller (.*)")
    fun useCaller(caller: String) {
        this.caller = caller
    }

    @When("I request a greeting")
    fun requestGreeting() {
        this.response = restTemplate.getForEntity("/greetings/{caller}", Greeting::class.java, caller)
    }

    @Then("I should get a response with HTTP status code (.*)")
    fun shouldGetResponseWithHttpStatusCode(statusCode: Int) {
        assertThat(response!!.statusCodeValue).isEqualTo(statusCode)
    }

    @And("The response should contain the message (.*)")
    fun theResponseShouldContainTheMessage(message: String) {
        assertThat(response!!.body.name).isEqualTo(message)
    }

}
