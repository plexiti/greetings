package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.Command
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
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
class CommandReceiver {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-commands-queue"))
    fun handle(@Payload json: String) {
        Command.issue(Command.toCommand(json))
    }

    @Bean
    fun commandsQueue(): Queue {
        return Queue("${context}-commands-queue", true)
    }

}
