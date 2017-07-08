package com.plexiti.flows.adapters.flow

import com.plexiti.commons.application.*
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Name
import com.plexiti.flows.application.FlowApplication
import org.assertj.core.api.Assertions.*
import org.camunda.bpm.engine.test.Deployment
import org.camunda.bpm.engine.test.ProcessEngineRule
import org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Deployment(resources = arrayOf("com/plexiti/flows/adapters/flow/FlowHandlerTest.bpmn"))
class FlowHandlerTest {

    var rule = ProcessEngineRule() @Rule get
    val handler = FlowMessageCorrelator()
    val application = FlowApplication()

    @Before
    fun init() {
        handler.flow = application
        application.runtimeService = rule.runtimeService
    }

    @Test
    fun deployment() {
        assertThat(rule.processEngine).isNotNull()
    }

    @Test
    fun startFlowViaCommand() {

        val flow = Flow(Name("Flow_FlowHandlerTest"))
        val message = FlowIO(flow, CommandId(UUID.randomUUID().toString()))

        handler.handle(message.toJson())

        val pi = rule.processEngine.runtimeService.createProcessInstanceQuery().singleResult();

        ProcessEngineAssertions.assertThat(pi)
            .isNotNull()
            .hasPassed("NoneStartEvent")
            .hasVariables("FlowHandlerTest")

    }

    @Test
    fun startFlowViaEvent() {

        val event = Event(Name("Flow_Start"))
        val flow = Flow(Name("Flow_FlowHandlerTest"))
        val message = FlowIO(flow, CommandId(UUID.randomUUID().toString()))
        message.event = event

        handler.handle(message.toJson())

        val pi = rule.processEngine.runtimeService.createProcessInstanceQuery().singleResult();

        ProcessEngineAssertions.assertThat(pi)
            .isNotNull()
            .hasPassed("MessageStartEvent")
            .hasVariables("FlowHandlerTest", "Flow_Start")

    }

    @Test
    fun correlateEvent() {

        startFlowViaCommand()

        val pi = rule.processEngine.runtimeService.createProcessInstanceQuery().singleResult();

        val event = Event(Name("Flow_Intermediate"))
        val message = FlowIO(event, CommandId(pi.businessKey))

        handler.handle(message.toJson())

        ProcessEngineAssertions.assertThat(pi)
            .isNotNull
            .hasPassed("EndEvent")
            .hasVariables("FlowHandlerTest", "Flow_Intermediate")

    }


    @Test
    fun correlateNonListeningEvent() {

        startFlowViaCommand()

        val pi = rule.processEngine.runtimeService.createProcessInstanceQuery().singleResult();

        val event = Event(Name("Flow_Intermediate2"))
        val message = FlowIO(event, CommandId(pi.businessKey))

        handler.handle(message.toJson())

        ProcessEngineAssertions.assertThat(pi)
            .isNotNull
            .hasNotPassed("EndEvent")
            .hasVariables("FlowHandlerTest", "Flow_Intermediate2")

    }

}
