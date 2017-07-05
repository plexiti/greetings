package com.plexiti.greetings.adapters

import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.greetings.application.GreetingApplication
import com.plexiti.greetings.domain.GreetingRepository
import com.plexiti.greetings.application.GreetingResource
import cucumber.api.java.en.And
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import org.assertj.core.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import java.io.File

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@ContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GreetingResourceSteps {

    @Value("\${com.plexiti.greetings.path}")
    lateinit var greetingsPath: String

    @Autowired
    lateinit var greetingRepository: GreetingRepository

    @Autowired
    lateinit var commandStore: CommandStore

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    var caller: String? = null
    var response: ResponseEntity<GreetingResource>? = null

    @Given("I use the caller (.*)")
    fun useCaller(caller: String) {
        this.caller = caller
    }

    @When("I request a greeting")
    fun requestGreeting() {
        this.response = restTemplate.getForEntity("/greetings/{caller}", GreetingResource::class.java, caller)
    }

    @Then("I should get a response with HTTP status code (.*)")
    fun shouldGetResponseWithHttpStatusCode(statusCode: Int) {
        assertThat(response!!.statusCodeValue).isEqualTo(statusCode)
    }

    @And("The response should contain the message (.*)")
    fun theResponseShouldContainTheMessage(message: String) {
        assertThat(response!!.body.greeting).isEqualTo(message)
    }

    @When("I place a greeting in the hot folder")
    fun placeGreeting() {
        val file = File(greetingsPath + "/cucumberTest")
        if (file.exists())
            file.delete()
        val writer = file.writer()
        writer.write(this.caller)
        writer.flush()
        writer.close()
        Thread.sleep(1000)
    }

    @Then("A command with the caller (.*) should be stored")
    fun theCommandShouldBeStoredInTheDatabase(caller: String) {
        val all = commandStore.findAll()
        val actual = all.filter { it is GreetingApplication.AnswerCaller && it.caller == caller }
        assertThat(actual).hasSize(1);
    }

    @Then("A greeting with the (.*) should be stored")
    fun theGreetingShouldBeStoredInTheDatabase(message: String) {
        val all = greetingRepository.findAll()
        val actual = all.filter { it.greeting.equals(message) }
        assertThat(actual).hasSize(1);
    }


}
