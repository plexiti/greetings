package com.plexiti.flows.adapters.flow

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.application.FlowIO
import com.plexiti.commons.domain.MessageType
import org.assertj.core.api.Assertions.*
import org.camunda.bpm.engine.test.Deployment
import org.camunda.bpm.engine.test.ProcessEngineRule
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
@Deployment(resources = arrayOf("com/plexiti/flows/adapters/flow/FlowEventTest.bpmn"))
class FlowEventTest {

    var rule = ProcessEngineRule() @Rule get
    val raiser = FlowEventRaiser()
    val json = ArgumentCaptor.forClass(String::class.java)

    @Before
    fun init() {
        Mocks.register("event", raiser)
        raiser.queue = "test"
        raiser.rabbit = mock(RabbitTemplate::class.java)
    }

    @Test
    fun deployment() {
        assertThat(rule.processEngine).isNotNull()
    }

    @Test
    fun happyPath() {

        rule.processEngine
            .runtimeService.startProcessInstanceByKey("FlowEventTest", "aBusinessKey")

        verify(raiser.rabbit, times(1)).convertAndSend(eq(raiser.queue), json.capture())

        val request = ObjectMapper().readValue(json.value, FlowIO::class.java)

        val event = request.event!!
        assertThat(event.type).isEqualTo(MessageType.Event)
        assertThat(event.name.qualified).isEqualTo("Flow_Test")
        assertThat(event.id).isNotNull()
        assertThat(event.raisedAt).isNotNull()
        assertThat(request.flowId).isNotNull()
        assertThat(request.tokenId).isNull()

    }

}
