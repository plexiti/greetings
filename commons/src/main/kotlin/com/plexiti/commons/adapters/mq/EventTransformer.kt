package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.Command
import com.plexiti.commons.domain.Event
import org.apache.camel.ProducerTemplate
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
import javax.transaction.Transactional


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class EventTransformer {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    private lateinit var route: ProducerTemplate

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-events-queue"))
    @Transactional
    fun handle(@Payload json: String) {
        val event = Event.toEvent(json)
        Command.correlateBy(event)?.correlate(event)
        Command.triggerBy(event).forEach {
            Command.async(it)
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
