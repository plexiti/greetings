package com.plexiti.commons.adapters.flow

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.application.Document
import com.plexiti.commons.application.FlowMessage
import com.plexiti.commons.application.Result
import com.plexiti.commons.domain.MessageType
import com.plexiti.commons.domain.Name
import com.plexiti.commons.domain.Problem
import org.assertj.core.api.Assertions.*
import org.camunda.bpm.engine.test.Deployment
import org.camunda.bpm.engine.test.ProcessEngineRule
import org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions
import org.camunda.bpm.engine.test.mock.Mocks
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.mockito.ArgumentCaptor


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Deployment(resources = arrayOf("com/plexiti/commons/adapters/flow/FlowCommandTest.bpmn"))
class FlowCommandTest {

    var rule = ProcessEngineRule() @Rule get
    val issuer = FlowCommandIssuer()
    val handler = FlowHandler()
    val json = ArgumentCaptor.forClass(String::class.java)

    @Before
    fun init() {
        Mocks.register("command", issuer)
        issuer.queue = "test"
        issuer.rabbitTemplate = mock(RabbitTemplate::class.java)
        handler.runtimeService = rule.runtimeService
    }

    @Test
    fun deployment() {
        assertThat(rule.processEngine).isNotNull()
    }

    @Test
    fun happyPath() {

        rule.processEngine
            .runtimeService.startProcessInstanceByKey("FlowCommandTest", "aBusinessKey")

        verify(issuer.rabbitTemplate, times(1)).convertAndSend(eq(issuer.queue), json.capture())

        val request = ObjectMapper().readValue(json.value, FlowMessage::class.java)

        val command = request.command!!
        assertThat(command.type).isEqualTo(MessageType.Command)
        assertThat(command.name.qualified).isEqualTo("Flow_Test")
        assertThat(command.id).isNotNull()
        assertThat(command.issuedAt).isNotNull()
        assertThat(request.tokenId).isNotNull()

        val pi = rule.processEngine.runtimeService.createProcessInstanceQuery().singleResult()
        assertThat(pi).isNotNull()

        val response = FlowMessage(Result(command), request.flowId, request.tokenId)

        handler.handle(response.toJson())

        ProcessEngineAssertions.assertThat(pi)
            .hasPassed("SuccessFulEndEvent")
            .hasVariables("Flow_Test")

    }

    @Test
    fun errorPath() {

        rule.processEngine
            .runtimeService.startProcessInstanceByKey("FlowCommandTest", "aBusinessKey")

        verify(issuer.rabbitTemplate, times(1)).convertAndSend(eq(issuer.queue), json.capture())

        val request = ObjectMapper().readValue(json.value, FlowMessage::class.java)
        val command = request.command!!
        val result = Result(command)
        result.problem = Problem("someError", "someMessage")
        val response = FlowMessage(result, request.flowId, request.tokenId)

        val pi = rule.processEngine.runtimeService.createProcessInstanceQuery().singleResult()

        handler.handle(response.toJson())

        ProcessEngineAssertions.assertThat(pi)
            .hasPassed("FailureEndEvent")
            .hasVariables("Flow_Test", "someError")

    }

    @Test
    fun happyResult() {

        rule.processEngine
            .runtimeService.startProcessInstanceByKey("FlowCommandTest", "aBusinessKey")

        verify(issuer.rabbitTemplate, times(1)).convertAndSend(eq(issuer.queue), json.capture())

        val request = ObjectMapper().readValue(json.value, FlowMessage::class.java)
        val command = request.command!!
        val result = Result(command)
        result.document = object: Document {
            override val name: Name
                get() = Name("Flow_Document")
            val someProperty =  "someValue"
        }
        val response = FlowMessage(result, request.flowId, request.tokenId)

        val pi = rule.processEngine.runtimeService.createProcessInstanceQuery().singleResult()

        handler.handle(response.toJson())

        ProcessEngineAssertions.assertThat(pi)
            .hasPassed("SuccessFulEndEvent")
            .hasVariables("Flow_Test", "Flow_Document")

    }

}
