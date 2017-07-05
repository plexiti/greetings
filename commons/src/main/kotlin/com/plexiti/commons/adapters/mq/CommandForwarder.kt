package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.StoredCommand
import org.apache.camel.Handler
import org.apache.camel.builder.RouteBuilder
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class CommandForwarder : RouteBuilder() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    val options = "consumer.namedQuery=${CommandForwarder::class.simpleName}&consumeDelete=false"

    override fun configure() {
        from("jpa:${StoredCommand::class.qualifiedName}?${options}")
            .bean(this)
    }

    @Handler
    fun send(storedCommand: StoredCommand) {
        rabbitTemplate.convertAndSend("${storedCommand.name.context}-commands-queue", storedCommand.json);
        logger.info("Forwarded ${storedCommand.json}")
    }

}
