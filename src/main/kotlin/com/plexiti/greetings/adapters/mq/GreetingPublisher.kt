package com.plexiti.greetings.adapters.mq

import org.apache.camel.Handler
import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
class GreetingPublisher {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Handler
    fun publish(message: String) {
        rabbitTemplate.convertAndSend("${context}-topic", message);
    }

    @Bean
    fun exchange(): TopicExchange {
        return TopicExchange("${context}-topic", true, false)
    }

}
