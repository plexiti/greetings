package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.CommandEntity
import org.apache.camel.Handler
import org.apache.camel.builder.RouteBuilder
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var context: String? = null
        @Value("\${com.plexiti.app.context}")
        set(value) {
            field = value
            queue = "${value}-commands-queue"
        }

    private lateinit var queue: String

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    val options = "consumer.namedQuery=CommandQueuer&consumeDelete=false"

    override fun configure() {
        from("jpa:${CommandEntity::class.qualifiedName}?${options}")
            .bean(this)
    }

    @Handler
    fun send(command: CommandEntity) {
        rabbitTemplate.convertAndSend(queue, command.json);
        logger.info("Queued ${command.json}")
    }

}