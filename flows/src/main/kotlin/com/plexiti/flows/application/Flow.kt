package com.plexiti.flows.application

import com.plexiti.commons.application.FlowMessage
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
    fun start(message: FlowMessage, json: SpinJsonNode) {
        val command = message.command!!
        val trigger = if (!message.events.isEmpty()) message.events.first() else null
        if (trigger != null) {
            runtimeService.startProcessInstanceByMessage(trigger.name.qualified,
                command.id.value,
                Variables.createVariables()
                    .putValue(trigger.name.qualified, json.prop("events").elements()[0])
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
    fun complete(message: FlowMessage, json: SpinJsonNode) {
        val result = message.result
        val command = result!!.command
        val variables = Variables.createVariables().putValue(command.name.qualified, json.prop("result"))
        if (result.problem != null) {
            variables.putValue(result.problem!!.code, json.prop("result").prop("problem"))
            runtimeService.signal(message.tokenId!!.value, result.problem!!.code, result.problem!!.message, variables)
        } else {
            if (result.document != null) {
                val document = json.prop("result").prop("document")
                variables.put(document.prop("name").stringValue(), document)
            }
            var idx = 0
            result.events?.forEach {
                val event = json.prop("result").prop("events").elements()[idx++]
                variables.put(it.name.qualified, event)
            }
            runtimeService.signal(message.tokenId!!.value, variables)
        }
    }

    @Transactional
    fun correlate(message: FlowMessage, json: SpinJsonNode) {
        val event = message.event!!
        try {
            runtimeService.createMessageCorrelation(event.name.qualified)
                .processInstanceBusinessKey(message.flowId.value)
                .setVariable(event.name.qualified, json.prop("event"))
                .correlateExclusively();
        } catch (e: MismatchingMessageCorrelationException) {
            val tokenId = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(message.flowId.value).singleResult()?.id
            if (tokenId != null) {
                runtimeService.setVariable(tokenId, event.name.qualified, json.prop("event"))
            }
        }
    }

}
