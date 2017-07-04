package com.plexiti.commons.adapters.mq

import com.plexiti.commons.adapters.db.CommandRepository
import com.plexiti.commons.adapters.db.EventRepository
import com.plexiti.commons.application.FlowEntity
import com.plexiti.commons.application.FlowMessage
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
class FlowForwarder : RouteBuilder() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var context: String? = null
        @Value("\${com.plexiti.app.context}")
        set(value) {
            field = value
            queue = "${value}-flows-to-queue"
        }

    private lateinit var queue: String

    @Autowired
    private lateinit var commands: CommandRepository

    @Autowired
    private lateinit var events: EventRepository

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    val options = "consumer.namedQuery=${FlowForwarder::class.simpleName}&consumeDelete=false"

    override fun configure() {
        from("jpa:${FlowEntity::class.qualifiedName}?${options}")
            .bean(this)
    }

    @Handler
    fun forward(flow: FlowEntity) {
        val message = FlowMessage(commands.findOne(flow.id)!!, flow.id)
        val event = if (flow.triggeredBy != null) events.findOne(flow.triggeredBy) else null
        if (event != null)
            message.history = listOf(event)
        rabbitTemplate.convertAndSend(queue, message.toJson());
        logger.info("Forwarded ${message.toJson()}")
    }

}
