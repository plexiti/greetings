package com.plexiti.commons.adapters.mq

import com.plexiti.commons.application.Command
import com.plexiti.greetings.application.GreetingApplication.*
import org.apache.camel.ProducerTemplate
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.camunda.spin.json.SpinJsonNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class CommandIssuer: AbstractBpmnActivityBehavior() {

    @Autowired
    lateinit var router: ProducerTemplate

    override fun execute(execution: ActivityExecution) {
        val json = execution.getVariable("callAnsweredAutomatically") as SpinJsonNode
        val command = Identify(json.prop("greeting").stringValue())
        execution.setVariableLocal("commandId", command.id)
        Command.issue(command)
        router.sendBody("direct:${command.type}", command)
    }

    override fun signal(execution: ActivityExecution, signalName: String?, signalData: Any?) {
        execution.removeVariable("commandId")
        leave(execution)
    }

}
