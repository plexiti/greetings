package com.plexiti.commons.adapters.flow

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.plexiti.commons.application.*
import com.plexiti.commons.domain.Event
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

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-flows-queue"))
    fun handle(json: String) {
        when(type(json)) {
            MessageType.Command -> command(json)
            MessageType.Event -> event(json)
            MessageType.Flow -> flow(json)
        }
    }

    @Transactional
    fun command(json: String) {
        val command = Command.fromJson(json, Command::class)
        val tokenId: TokenId? = null // TODO
        runtimeService.signal(tokenId!!.value, null, JSON(command), null)
    }

    @Transactional
    fun event(json: String) {
        val event = Event.fromJson(json, Event::class)
        val flowId: CommandId? = null // TODO
        try {
            val correlation: Correlation? = null  // TODO
            runtimeService.createMessageCorrelation(correlation!!.value)
                .processInstanceBusinessKey(flowId!!.value)
                .setVariable(event.name.qualified, JSON(event))
                .correlateExclusively();
        } catch (e: MismatchingMessageCorrelationException) {
            val tokenId = null // TODO
            runtimeService.setVariable(tokenId, event.name.qualified, JSON(event))
        }
    }

    @Transactional
    fun flow(json: String) {
        val flow = Command.fromJson(json, Flow::class)
        val trigger: Event? = null // TODO
        if (trigger != null) {
            runtimeService.startProcessInstanceByMessage(trigger.name.qualified,
                flow.id.value,
                Variables.createVariables()
                    .putValue(trigger.name.qualified, JSON(trigger))
                    .putValue(flow.name.qualified, JSON(flow))
            )
        } else {
            runtimeService.startProcessInstanceByKey(flow.name.qualified,
                flow.id.value,
                Variables.createVariables()
                    .putValue(flow.name.qualified, JSON(flow)))
        }
    }

    @Bean
    fun flowsQueue(): Queue {
        return Queue("${context}-flows-queue", true)
    }

    private fun type(json: String): MessageType? {
        try {
            val node = ObjectMapper().readValue(json, ObjectNode::class.java)
            val type = node.get("messageType").get("tokenId").textValue()
            return if (type != null) MessageType.valueOf(type) else null
        } catch (ex: JsonMappingException) {
            return null
        }
    }

}
