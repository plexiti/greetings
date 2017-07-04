package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.ApplicationService
import com.plexiti.commons.application.Command
import com.plexiti.commons.domain.Name
import org.springframework.amqp.core.Queue
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


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class CommandExecutor {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    private lateinit var applicationService: ApplicationService

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-commands-queue"))
    fun executeCommand(@Payload json: String) {
        applicationService.executeCommand(json)
    }

    @Bean
    fun commandsQueue(): Queue {
        return Queue("${context}-commands-queue", true)
    }

    /*
    @Bean
    fun beanFactoryPostProcessor(): BeanFactoryPostProcessor = BeanFactoryPostProcessor {
        val names = mutableSetOf<Name>()
        Command.types.forEach { name, _ ->
            if (!names.contains(name)) {
                names.add(name)
                (it as BeanDefinitionRegistry).registerBeanDefinition("commandsQueue${name.context}",
                    BeanDefinitionBuilder.genericBeanDefinition(Queue::class.java)
                        .addConstructorArgValue("${name.context}-commands-queue")
                        .addConstructorArgValue(true)
                        .beanDefinition
                )
            }
        }
    }
    */

}
