package com.plexiti.greetings.adapters.mq

import com.plexiti.commons.domain.EventEntity
import org.apache.camel.Handler
import org.apache.camel.builder.RouteBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component @Configuration
class GreetingPublisher : RouteBuilder() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var context: String? = null
        @Value("\${com.plexiti.app.context}")
        set(value) {
            field = value
            topic = "${value}-events"
        }

    private lateinit var topic: String

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    val options = "consumer.namedQuery=EventPublisher&consumeDelete=false"

    override fun configure() {
        from("jpa:${EventEntity::class.qualifiedName}?${options}")
            .bean(this)
    }

    @Handler
    fun publish(event: EventEntity) {
        rabbitTemplate.convertAndSend(topic, event.properties);
        logger.info("Published ${event.properties}")
    }

    @Bean
    fun exchange(): TopicExchange {
        return TopicExchange(topic, true, false)
    }

}
