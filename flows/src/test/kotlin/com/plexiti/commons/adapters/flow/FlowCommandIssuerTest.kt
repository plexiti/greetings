package com.plexiti.commons.adapters.flow

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.application.Command
import com.plexiti.commons.application.FlowMessage
import com.plexiti.commons.domain.MessageType
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
@Deployment(resources = arrayOf("com/plexiti/commons/adapters/flow/FlowCommandIssuer.bpmn"))
class FlowCommandIssuerTest {

    var rule = ProcessEngineRule() @Rule get
    val behavior = FlowCommandIssuer()
    val json = ArgumentCaptor.forClass(String::class.java)

    @Before
    fun init() {
        Mocks.register("command", behavior)
        behavior.queue = "test"
        behavior.rabbitTemplate = mock(RabbitTemplate::class.java)
    }

    @Test
    fun deployment() {
        assertThat(rule.processEngine).isNotNull()
    }

    @Test
    fun leave() {

        rule.processEngine
            .runtimeService.startProcessInstanceByKey("FlowCommandIssuer", "aBusinessKey")

        verify(behavior.rabbitTemplate, times(1)).convertAndSend(eq(behavior.queue), json.capture())

        val command = ObjectMapper().readValue(json.value, FlowMessage::class.java).message as Command
        assertThat(command.type).isEqualTo(MessageType.Command)
        assertThat(command.name.qualified).isEqualTo("Flow_Test")
        assertThat(command.id).isNotNull()
        assertThat(command.issuedAt).isNotNull()
        assertThat(command.internals.tokenId).isNotNull()

        val pi = rule.processEngine.runtimeService.createProcessInstanceQuery().singleResult()
        assertThat(pi).isNotNull()

        rule.processEngine.runtimeService.signal(command.internals.tokenId!!.value)

        ProcessEngineAssertions.assertThat(pi).hasPassed("SuccessFulEndEvent")

    }

}
