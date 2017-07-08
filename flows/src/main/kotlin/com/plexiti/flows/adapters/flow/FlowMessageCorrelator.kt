package com.plexiti.flows.adapters.flow

import com.plexiti.commons.application.*
import com.plexiti.commons.domain.MessageType
import com.plexiti.flows.application.FlowApplication
import org.camunda.spin.json.SpinJsonNode.JSON
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class FlowMessageCorrelator {

    private val logger = LoggerFactory.getLogger("com.plexiti.flows")

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    lateinit var flow: FlowApplication

    @RabbitListener(queues = arrayOf("\${com.plexiti.app.context}-flows-to-queue"))
    fun handle(json: String) {
        val message = FlowIO.fromJson(json)
        when(message.type) {
            MessageType.Flow -> {
                flow.start(message, JSON(json))
                logger.info("Started ${json}")
            }
            MessageType.Event -> {
                flow.correlate(message, JSON(json))
                logger.info("Correlated ${json}")
            }
            MessageType.Document ->  {
                flow.complete(message, JSON(json))
                logger.info("Completed ${json}")
            }
        }
    }

    @Bean
    fun toFlowsQueue(): Queue {
        return Queue("${context}-flows-to-queue", true)
    }

}
