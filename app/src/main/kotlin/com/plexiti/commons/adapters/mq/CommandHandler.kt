package com.plexiti.commons.adapters.mq

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class CommandHandler {

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
