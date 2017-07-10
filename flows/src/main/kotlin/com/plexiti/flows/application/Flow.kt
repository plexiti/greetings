package com.plexiti.flows.application

import com.plexiti.commons.application.FlowIO
import org.camunda.bpm.engine.MismatchingMessageCorrelationException
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.variable.Variables
import org.camunda.spin.json.SpinJsonNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class FlowApplication {

    @Autowired
    lateinit var runtimeService: RuntimeService

    @Transactional
    fun start(io: FlowIO, json: SpinJsonNode) {
        val command = io.command!!
        val trigger = io.event
        if (trigger != null) {
            runtimeService.startProcessInstanceByMessage(trigger.name.qualified,
                command.id.value,
                Variables.createVariables()
                    .putValue(trigger.name.qualified, json.prop("event"))
                    .putValue(command.name.name, json.prop("command"))
            )
        } else {
            runtimeService.startProcessInstanceByKey(command.name.name,
                command.id.value,
                Variables.createVariables()
                    .putValue(command.name.name, json.prop("command")))
        }
    }

    @Transactional
    fun complete(io: FlowIO, json: SpinJsonNode) {
        val result = io.document
        val command = result!!.command
        val variables = Variables.createVariables().putValue(command.name.qualified, json.prop("document"))
        if (result.problemOccured != null) {
            variables.putValue(result.problemOccured!!.code, json.prop("document").prop("problemOccured"))
            runtimeService.signal(io.tokenId!!.value, result.problemOccured!!.code, result.problemOccured!!.message, variables)
        } else {
            if (result.valueReturned != null) {
                val document = json.prop("document").prop("valueReturned")
                variables.put(document.prop("name").stringValue(), document)
            }
            var idx = 0
            result.events?.forEach {
                val event = json.prop("document").prop("events").elements()[idx++]
                variables.put(it.name.qualified, event)
            }
            runtimeService.signal(io.tokenId!!.value, variables)
        }
    }

    @Transactional
    fun correlate(io: FlowIO, json: SpinJsonNode) {
        val event = io.event!!
        try {
            runtimeService.createMessageCorrelation(event.name.qualified)
                .processInstanceBusinessKey(io.flowId.value)
                .setVariable(event.name.qualified, json.prop("event"))
                .correlateExclusively();
        } catch (e: MismatchingMessageCorrelationException) {
            val tokenId = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(io.flowId.value).singleResult()?.id
            if (tokenId != null) {
                runtimeService.setVariable(tokenId, event.name.qualified, json.prop("event"))
            }
        }
    }

}
