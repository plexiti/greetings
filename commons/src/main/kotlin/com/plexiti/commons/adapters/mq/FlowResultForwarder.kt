package com.plexiti.commons.adapters.mq

import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.application.StoredCommand
import com.plexiti.commons.application.FlowIO
import com.plexiti.commons.application.Document
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
class FlowResultForwarder : RouteBuilder() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var context: String? = null
        @Value("\${com.plexiti.app.context}")
        set(value) {
            field = value
            queue = "${value}-flows-to-queue"
        }

    private lateinit var queue: String

    @Autowired
    private lateinit var commands: CommandStore

    @Autowired
    private lateinit var events: EventStore

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    val options = "consumer.namedQuery=${FlowResultForwarder::class.simpleName}&consumeDelete=false"

    override fun configure() {
        from("jpa:${StoredCommand::class.qualifiedName}?${options}")
            .bean(this)
    }

    @Handler
    fun forward(storedCommand: StoredCommand) {
        val message = FlowIO(Document(commands.findOne(storedCommand.id)!!), storedCommand.issuedBy!!, storedCommand.correlatedToToken!!)
        rabbitTemplate.convertAndSend(queue, message.toJson());
        logger.info("Forwarded ${message.toJson()}")
    }

}
