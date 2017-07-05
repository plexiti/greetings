package com.plexiti.flows.adapters.flow

import com.plexiti.commons.application.*
import com.plexiti.commons.domain.Name
import com.plexiti.flows.util.property
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
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
            queue = "${value}-flows-from-queue"
        }

    internal lateinit var queue: String

    @Autowired
    internal lateinit var rabbit: RabbitTemplate

    override fun execute(execution: ActivityExecution) {
        val command = FlowMessage(
            Command(Name(property("command", execution.bpmnModelElementInstance))),
            CommandId(execution.processBusinessKey),
            TokenId(execution.id))
        val json = command.toJson()
        rabbit.convertAndSend(queue, json);
        logger.info("Transfered ${json}")
    }

    override fun signal(execution: ActivityExecution, signalName: String?, signalData: Any?) {
        if (signalName == null) leave(execution) else propagateBpmnError(BpmnError(signalName, signalData as String?), execution)
    }

}
