package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.FlowService
import org.springframework.amqp.core.Queue
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
class FlowCommandExecutor {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    private lateinit var flowService: FlowService

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-flow-commands-queue"))
    fun executeCommand(@Payload json: String) {
        flowService.executeCommand(json)
    }

    @Bean
    fun flowCommandsQueue(): Queue {
        return Queue("${context}-flow-commands-queue", true)
    }

}
