package com.plexiti.commons.adapters.flow

import com.plexiti.commons.application.*
import com.plexiti.commons.domain.MessageType
import org.camunda.bpm.engine.MismatchingMessageCorrelationException
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.variable.Variables
import org.camunda.spin.Spin.*
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
        when(message.command?.type) {
            MessageType.Command -> command(message)
            MessageType.Flow -> flow(message)
            else -> event(message)
        }
    }

    @Transactional
    fun command(message: FlowMessage) {
        val command = message.command!!
        val variables = Variables.createVariables().putValue(command.name.qualified, JSON(command))
        message.history.forEach {
            variables.put(it.name.qualified, JSON(it))
        }
        runtimeService.signal(message.tokenId!!.value, variables)
    }

    @Transactional
    fun event(message: FlowMessage) {
        val event = message.event!!
        try {
            runtimeService.createMessageCorrelation(event.name.qualified)
                .processInstanceBusinessKey(message.flowId.value)
                .setVariable(event.name.qualified, JSON(event))
                .correlateExclusively();
        } catch (e: MismatchingMessageCorrelationException) {
            runtimeService.setVariable(message.flowId.value, event.name.qualified, JSON(event))
        }
    }

    @Transactional
    fun flow(message: FlowMessage) {
        val command = message.command!!
        val trigger = if (!message.history.isEmpty()) message.history.first() else null
        if (trigger != null) {
            runtimeService.startProcessInstanceByMessage(trigger.name.qualified,
                command.id.value,
                Variables.createVariables()
                    .putValue(trigger.name.qualified, JSON(trigger))
                    .putValue(command.name.qualified, JSON(message.command))
            )
        } else {
            runtimeService.startProcessInstanceByKey(command.name.qualified,
                command.id.value,
                Variables.createVariables()
                    .putValue(command.name.qualified, JSON(command)))
        }
    }

    @Bean
    fun toFlowsQueue(): Queue {
        return Queue("${context}-flows-to-queue", true)
    }

}
