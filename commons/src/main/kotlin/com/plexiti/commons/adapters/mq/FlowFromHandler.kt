package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.Application
import org.springframework.amqp.core.Queue
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
class FlowFromHandler {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    private lateinit var application: Application

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-flows-from-queue"))
    @Transactional
    fun handle(@Payload json: String) {
        application.handle(json)
    }

    @Bean
    fun eventsQueue(): Queue {
        return Queue("${context}-events-queue", true)
    }

    @Bean
    fun fromFlowsQueue(): Queue {
        return Queue("${context}-flows-from-queue", true)
    }

}
