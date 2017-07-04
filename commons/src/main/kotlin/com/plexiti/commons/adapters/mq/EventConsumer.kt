package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.ApplicationService
import com.plexiti.commons.application.Command
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Name
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
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
class EventConsumer {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    private lateinit var applicationService: ApplicationService

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-events-queue"))
    @Transactional
    fun handle(@Payload json: String) {
        applicationService.consumeEvent(json)
    }

    @Bean
    fun eventsQueue(): Queue {
        return Queue("${context}-events-queue", true)
    }

    @Bean
    fun binding(eventsQueue: Queue, eventsTopic: TopicExchange): Binding {
        return BindingBuilder.bind(eventsQueue).to(eventsTopic).with(context)
    }

    /*
    @Bean
    fun beanFactoryPostProcessor(): BeanFactoryPostProcessor = BeanFactoryPostProcessor {
        val names = mutableSetOf<Name>()
        Event.types.forEach { name, _ ->
            if (!names.contains(name)) {
                names.add(name)
                (it as BeanDefinitionRegistry).registerBeanDefinition("eventsTopic${name.context}",
                    BeanDefinitionBuilder.genericBeanDefinition(Binding::class.java)
                        .addConstructorArgValue("${context}-events-queue")
                        .addConstructorArgValue(Binding.DestinationType.QUEUE)
                        .addConstructorArgValue("${name.context}-events-topic")
                        .addConstructorArgValue(name.context)
                        .addConstructorArgValue(emptyMap<String, Object>())
                        .beanDefinition
                )
            }
        }
    }
    */

}
