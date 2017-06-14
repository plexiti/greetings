package com.plexiti.commons.adapters.mq

import com.plexiti.commons.domain.Event
import org.camunda.bpm.engine.MismatchingMessageCorrelationException
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.spin.json.SpinJsonNode.JSON
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class EventHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    lateinit var flowControl: ProcessEngine

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-queue"))
    fun handle(@Payload json: String) {

        val event = Event.toEvent(json)

        try {
            flowControl.runtimeService
                .createMessageCorrelation(event.type)
                .setVariable(event.type, JSON(json))
                .correlateStartMessage();
            logger.debug("Event WAS correlated: ${json}")
        } catch (m: MismatchingMessageCorrelationException) {
            logger.debug("Event NOT correlated: ${json}")
        }

    }

    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(context)
    }

}
