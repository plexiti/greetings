package com.plexiti.commons.adapters.mq

import com.plexiti.commons.domain.StoredEvent
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
import org.springframework.context.annotation.Profile


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class EventForwarder : RouteBuilder() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var context: String? = null
        @Value("\${com.plexiti.app.context}")
        set(value) {
            field = value
            topic = "${value}-events-topic"
        }

    private lateinit var topic: String

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    val options = "consumer.namedQuery=${EventForwarder::class.simpleName}&consumeDelete=false"

    override fun configure() {
        from("jpa:${StoredEvent::class.qualifiedName}?${options}")
            .bean(this)
    }

    @Handler
    fun publish(event: StoredEvent) {
        rabbitTemplate.convertAndSend(topic, context, event.json);
        logger.info("Forwarded ${event.json}")
    }

    @Bean
    fun eventsTopic(): TopicExchange {
        return TopicExchange(topic, true, false)
    }

    /*
    @Bean
    fun beanFactoryPostProcessor(): BeanFactoryPostProcessor = BeanFactoryPostProcessor {
        val names = mutableSetOf<Name>()
        Event.types.forEach { name, _ ->
            if (!names.contains(name)) {
                names.add(name)
                (it as BeanDefinitionRegistry).registerBeanDefinition("eventsTopic${name.context}",
                    BeanDefinitionBuilder.genericBeanDefinition(TopicExchange::class.java)
                        .addConstructorArgValue("${name.context}-events-topic")
                        .addConstructorArgValue(true)
                        .addConstructorArgValue(false)
                        .beanDefinition
                )
            }
        }
    }
    */

}
