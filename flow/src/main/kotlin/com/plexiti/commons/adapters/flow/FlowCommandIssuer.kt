package com.plexiti.commons.adapters.flow

import com.plexiti.commons.application.*
import com.plexiti.commons.domain.Name
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.camunda.spin.json.SpinJsonNode
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
@Component("command")
@Configuration
@Profile("prod")
class FlowCommandIssuer : AbstractBpmnActivityBehavior() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    internal var context: String? = null
        @Value("\${com.plexiti.app.context}")
        set(value) {
            field = value
            queue = "${value}-commands-queue"
        }

    internal lateinit var queue: String

    @Autowired
    internal lateinit var rabbitTemplate: RabbitTemplate

    fun send(command: Command) {
        rabbitTemplate.convertAndSend(queue, command.toJson());
        logger.info("Forwarded ${command.toJson()}")
    }

    override fun execute(execution: ActivityExecution) {

        val commandName = execution.bpmnModelElementInstance.domElement
            .childElements.find { it.localName == "extensionElements" }
            ?.childElements?.find { it.localName == "properties" }
            ?.childElements?.find { it.localName == "property" && it.hasAttribute("name") && it.getAttribute("name") == "command" }
            ?.getAttribute("value") ?: throw IllegalArgumentException("Command must be specified as <camunda:property name='command'/>)")

        val command = FlowCommand(
            Name(commandName),
            FlowId(execution.processInstanceId),
            TokenId(execution.id)
        )

        send(command)

    }

    override fun signal(execution: ActivityExecution, signalName: String?, signalData: Any?) {
        if (signalData is SpinJsonNode) {
            execution.setVariable(signalData.prop("name").stringValue(), signalData)
        }
        leave(execution)
    }

}
