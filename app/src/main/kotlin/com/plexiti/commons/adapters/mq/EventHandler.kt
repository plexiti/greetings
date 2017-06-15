package com.plexiti.commons.adapters.mq

import com.plexiti.commons.domain.Event
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.impl.event.EventType
import org.camunda.spin.json.SpinJsonNode.JSON
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

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    lateinit var flowControl: ProcessEngine

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-events-queue"))
    fun handle(@Payload json: String) {

        val event = Event.toEvent(json)

        // try to complete a command

        val commandExecution = flowControl.runtimeService
            .createExecutionQuery()
            .variableValueEquals("commandId", event.commandId)
            .singleResult();

        if (commandExecution != null) {
            flowControl.runtimeService
                .signal(commandExecution.id, mapOf(event.name to JSON(json)))
        }

        // try to correlate to a start message

        val eventSubscriptions = flowControl.runtimeService
            .createEventSubscriptionQuery()
            .eventType(EventType.MESSAGE.name())
            .eventName(event.name)
            .count();

        if (eventSubscriptions > 0) {
            flowControl.runtimeService
                .createMessageCorrelation(event.name)
                .setVariable(event.name, JSON(json))
                .correlateStartMessage();
        }

    }

    @Bean
    fun eventsQueue(): Queue {
        return Queue("${context}-events-queue", true)
    }

    @Bean
    fun binding(eventsQueue: Queue, eventsTopic: TopicExchange): Binding {
        return BindingBuilder.bind(eventsQueue).to(eventsTopic).with(context)
    }

}
