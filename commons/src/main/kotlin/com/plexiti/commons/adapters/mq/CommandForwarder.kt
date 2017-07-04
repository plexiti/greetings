package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandEntity
import com.plexiti.commons.domain.Name
import org.apache.camel.Handler
import org.apache.camel.builder.RouteBuilder
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.context.annotation.Bean
import kotlin.reflect.KClass


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
        from("jpa:${CommandEntity::class.qualifiedName}?${options}")
            .bean(this)
    }

    @Handler
    fun send(command: CommandEntity) {
        rabbitTemplate.convertAndSend("${command.name.context}-commands-queue", command.json);
        logger.info("Forwarded ${command.json}")
    }

}
