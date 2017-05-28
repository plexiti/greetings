package com.plexiti.greetings

import cucumber.api.java.en.And
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import org.assertj.core.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@ContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GreetingsResourceSteps {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    var caller: String? = null
    var response: ResponseEntity<String>? = null

    @Given("I use the caller (.*)")
    fun useCaller(caller: String) {
        this.caller = caller
    }

    @When("I request a greeting")
    fun requestGreeting() {
        this.response = restTemplate.exchange("/greetings/{caller}",
                HttpMethod.GET, null, String::class.java, caller)
    }

    @Then("I should get a response with HTTP status code (.*)")
    fun shouldGetResponseWithHttpStatusCode(statusCode: Int) {
        assertThat(response!!.statusCodeValue).isEqualTo(statusCode)
    }

    @And("The response should contain the message (.*)")
    fun theResponseShouldContainTheMessage(message: String) {
        assertThat(response!!.body).isEqualTo(message)
    }

}