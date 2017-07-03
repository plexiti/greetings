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
        when(message.message.type) {
            MessageType.Command -> command(message)
            MessageType.Event -> event(message)
            MessageType.Flow -> flow(message)
        }
    }

    @Transactional
    fun command(command: FlowMessage) {
        val variables = Variables.createVariables().putValue(command.message.name.qualified, JSON(command.message))
        command.history.forEach {
            variables.put(it.name.qualified, JSON(it))
        }
        runtimeService.signal(command.tokenId!!.value, variables)
    }

    @Transactional
    fun event(event: FlowMessage) {
        try {
            runtimeService.createMessageCorrelation(event.message.name.qualified)
                .processInstanceBusinessKey(event.flowId.value)
                .setVariable(event.message.name.qualified, JSON(event.message))
                .correlateExclusively();
        } catch (e: MismatchingMessageCorrelationException) {
            runtimeService.setVariable(event.flowId.value, event.message.name.qualified, JSON(event.message))
        }
    }

    @Transactional
    fun flow(flow: FlowMessage) {
        val trigger = if (!flow.history.isEmpty()) flow.history.first() else null
        if (trigger != null) {
            runtimeService.startProcessInstanceByMessage(trigger.name.qualified,
                flow.message.id.value,
                Variables.createVariables()
                    .putValue(trigger.name.qualified, JSON(trigger))
                    .putValue(flow.message.name.qualified, JSON(flow.message))
            )
        } else {
            runtimeService.startProcessInstanceByKey(flow.message.name.qualified,
                flow.message.id.value,
                Variables.createVariables()
                    .putValue(flow.message.name.qualified, JSON(flow.message)))
        }
    }

    @Bean
    fun toFlowsQueue(): Queue {
        return Queue("${context}-flows-to-queue", true)
    }

}
