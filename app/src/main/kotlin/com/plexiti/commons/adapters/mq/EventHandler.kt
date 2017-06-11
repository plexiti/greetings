package com.plexiti.commons.adapters.mq

import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.amqp.core.TopicExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Payload


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class EventHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @RabbitListener(queues = arrayOf("greetings-queue"))
    fun handle(@Payload message: String) {
        logger.info("Received $message")
    }

    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with("greetings")
    }

}
