package com.plexiti.flows.adapters.flow

import com.plexiti.commons.application.*
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Name
import com.plexiti.flows.util.property
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
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
@Component("event")
@Configuration
@Profile("prod")
class FlowEventQueuer : JavaDelegate {

    private val logger = LoggerFactory.getLogger("com.plexiti.flows.adapters")

    internal var context: String? = null
        @Value("\${com.plexiti.app.context}")
        set(value) {
            field = value
            queue = "${value}-flows-from-queue"
        }

    internal lateinit var queue: String

    @Autowired
    internal lateinit var rabbit: RabbitTemplate

    override fun execute(execution: DelegateExecution) {
        val event = FlowIO(
            Event(Name(property("event", execution.bpmnModelElementInstance))),
            CommandId(execution.processBusinessKey))
        rabbit.convertAndSend(queue, event.toJson());
        logger.info("Queued ${event.toJson()}")
    }

}
