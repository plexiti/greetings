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
class CommandQueuer : RouteBuilder() {

    private val logger = LoggerFactory.getLogger("com.plexiti.application")

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    val options = "consumer.namedQuery=${CommandQueuer::class.simpleName}&consumeDelete=false"

    override fun configure() {
        from("jpa:${StoredCommand::class.qualifiedName}?${options}")
            .bean(this)
    }

    @Handler
    fun queue(storedCommand: StoredCommand) {
        rabbitTemplate.convertAndSend("${storedCommand.name.context}-commands-queue", storedCommand.json);
        logger.info("Queued ${storedCommand.json}")
    }

}
