package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.Application
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class CommandExecutor {

    private val logger = LoggerFactory.getLogger("com.plexiti.application")

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    private lateinit var application: Application

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-commands-queue"))
    fun execute(@Payload json: String) {
        try {
            application.execute(json)
            logger.info("Executed ${json}")
        } catch (e: ObjectOptimisticLockingFailureException) {
            // TODO make a proper difference between ignoring known duplicates
            // (known) temporary failures, and permanent technical failures (bugs)
            logger.info("Deferred ${json}")
            throw e
        }
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
