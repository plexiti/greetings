package com.plexiti.greetings.adapters.mq

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
class GreetingHandler {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @RabbitHandler
    fun handle(message: String) {
        println("Received <$message>")
    }

    @Bean
    fun queue(): Queue {
        return Queue("${context}-queue", true)
    }

}
