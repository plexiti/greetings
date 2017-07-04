package com.plexiti.commons.adapters.flow

import com.plexiti.commons.application.*
import com.plexiti.commons.domain.MessageType
import org.camunda.bpm.engine.MismatchingMessageCorrelationException
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.variable.Variables
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
            MessageType.Flow -> flow(message)
            MessageType.Event -> event(message)
            MessageType.Result -> result(message)
        }
    }

    @Transactional
    fun result(message: FlowMessage) {
        val result = message.result
        val command = result!!.command
        val variables = Variables.createVariables().putValue(command.name.qualified, JSON(result.toJson()))
        if (result.problem != null) {
            variables.putValue(result.problem!!.code, JSON(result.problem!!.toJson()))
            runtimeService.signal(message.tokenId!!.value, result.problem!!.code, result.problem!!.message, variables)
        } else {
            if (result.document != null) {
                variables.put(result.document!!.name().qualified, JSON(result.document!!.toJson()))
            }
            result.events?.forEach {
                variables.put(it.name.qualified, JSON(it.toJson()))
            }
            runtimeService.signal(message.tokenId!!.value, variables)
        }
    }

    @Transactional
    fun event(message: FlowMessage) {
        val event = message.event!!
        try {
            runtimeService.createMessageCorrelation(event.name.qualified)
                .processInstanceBusinessKey(message.flowId.value)
                .setVariable(event.name.qualified, JSON(event.toJson()))
                .correlateExclusively();
        } catch (e: MismatchingMessageCorrelationException) {
            val tokenId = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(message.flowId.value).singleResult()?.id
            if (tokenId != null) {
                runtimeService.setVariable(tokenId, event.name.qualified, JSON(event.toJson()))
            }
        }
    }

    @Transactional
    fun flow(message: FlowMessage) {
        val command = message.command!!
        val trigger = if (!message.events.isEmpty()) message.events.first() else null
        if (trigger != null) {
            runtimeService.startProcessInstanceByMessage(trigger.name.qualified,
                command.id.value,
                Variables.createVariables()
                    .putValue(trigger.name.qualified, JSON(trigger.toJson()))
                    .putValue(command.name.qualified, JSON(command.toJson()))
            )
        } else {
            runtimeService.startProcessInstanceByKey(command.name.qualified,
                command.id.value,
                Variables.createVariables()
                    .putValue(command.name.qualified, JSON(command.toJson())))
        }
    }

    @Bean
    fun toFlowsQueue(): Queue {
        return Queue("${context}-flows-to-queue", true)
    }

}
