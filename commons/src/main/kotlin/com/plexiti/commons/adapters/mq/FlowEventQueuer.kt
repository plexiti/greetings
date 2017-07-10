package com.plexiti.commons.adapters.mq

import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.application.FlowIO
import com.plexiti.commons.domain.StoredEvent
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
class FlowEventQueuer : RouteBuilder() {

    private val logger = LoggerFactory.getLogger("com.plexiti.application")

    private var context: String? = null
        @Value("\${com.plexiti.app.context}")
        set(value) {
            field = value
            queue = "${value}-flows-to-queue"
        }

    private lateinit var queue: String

    @Autowired
    private lateinit var commandStore: CommandStore

    @Autowired
    private lateinit var eventStore: EventStore

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    val options = "consumer.namedQuery=${FlowEventQueuer::class.simpleName}&consumeDelete=false"

    override fun configure() {
        from("jpa:${StoredEvent::class.qualifiedName}?${options}")
            .bean(this)
    }

    @Handler
    fun queue(event: StoredEvent) {
        val commands = commandStore.findByEventsAssociated_Containing(event.id.value)
        commands.forEach {
            // TODO Consider that this could partially fail and result in duplicate messages

            val message = FlowIO(eventStore.findOne(event.id)!!, it.id)
            rabbitTemplate.convertAndSend(queue, message.toJson());
            logger.info("Queued ${message.toJson()}")
        }
    }

}
