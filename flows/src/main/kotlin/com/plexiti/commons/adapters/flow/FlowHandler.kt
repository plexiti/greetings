package com.plexiti.commons.adapters.flow

import com.plexiti.commons.application.*
import com.plexiti.commons.domain.MessageType
import org.camunda.bpm.engine.MismatchingMessageCorrelationException
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.variable.Variables
import org.camunda.spin.json.SpinJsonNode
import org.camunda.spin.json.SpinJsonNode.JSON
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class FlowHandler {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    lateinit var runtimeService: RuntimeService

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-flows-to-queue"))
    fun handle(json: String) {
        val message = FlowMessage.fromJson(json)
        when(message.type) {
            MessageType.Flow -> flow(message, JSON(json))
            MessageType.Event -> event(message, JSON(json))
            MessageType.Result -> result(message, JSON(json))
        }
    }

    @Transactional
    fun result(message: FlowMessage, json: SpinJsonNode) {
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
    fun event(message: FlowMessage, json: SpinJsonNode) {
        val event = message.event!!
        try {
            runtimeService.createMessageCorrelation(event.name.qualified)
                .processInstanceBusinessKey(message.flowId.value)
                .setVariable(event.name.qualified, json.prop("result").prop("event"))
                .correlateExclusively();
        } catch (e: MismatchingMessageCorrelationException) {
            val tokenId = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(message.flowId.value).singleResult()?.id
            if (tokenId != null) {
                runtimeService.setVariable(tokenId, event.name.qualified, json.prop("result").prop("event"))
            }
        }
    }

    @Transactional
    fun flow(message: FlowMessage, json: SpinJsonNode) {
        val command = message.command!!
        val trigger = if (!message.events.isEmpty()) message.events.first() else null
        if (trigger != null) {
            runtimeService.startProcessInstanceByMessage(trigger.name.qualified,
                command.id.value,
                Variables.createVariables()
                    .putValue(trigger.name.qualified, json.prop("result").prop("events").elements()[0])
                    .putValue(command.name.qualified, json.prop("result").prop("command"))
            )
        } else {
            runtimeService.startProcessInstanceByKey(command.name.qualified,
                command.id.value,
                Variables.createVariables()
                    .putValue(command.name.qualified, json.prop("result").prop("command")))
        }
    }

    @Bean
    fun toFlowsQueue(): Queue {
        return Queue("${context}-flows-to-queue", true)
    }

}
