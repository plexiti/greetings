package com.plexiti.commons.adapters.flow

import com.plexiti.commons.application.*
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Name
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
class FlowEventRaiser: JavaDelegate {

    private val logger = LoggerFactory.getLogger(this::class.java)

    internal var context: String? = null
        @Value("\${com.plexiti.app.context}")
        set(value) {
            field = value
            queue = "${value}-events-queue"
        }

    internal lateinit var queue: String

    @Autowired
    internal lateinit var rabbitTemplate: RabbitTemplate

    fun raise(event: Event) {
        rabbitTemplate.convertAndSend(queue, event.toJson());
        logger.info("Forwarded ${event.toJson()}")
    }

    override fun execute(execution: DelegateExecution) {

        val eventName = execution.bpmnModelElementInstance.domElement
            .childElements.find { it.localName == "extensionElements" }
            ?.childElements?.find { it.localName == "properties" }
            ?.childElements?.find { it.localName == "property" && it.hasAttribute("name") && it.getAttribute("name") == "event" }
            ?.getAttribute("value") ?: throw IllegalArgumentException("Event must be specified as <camunda:property name='event'/>)")

        val event = FlowEvent(
            Name(eventName),
            TokenId(execution.id)
        )

        raise(event)

    }

}
