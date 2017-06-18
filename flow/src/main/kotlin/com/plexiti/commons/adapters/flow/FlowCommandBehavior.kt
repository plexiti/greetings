package com.plexiti.commons.adapters.flow

import com.plexiti.commons.application.Command
import com.plexiti.commons.domain.Event
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.spin.json.SpinJsonNode.JSON
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.camunda.spin.json.SpinJsonNode
import org.slf4j.LoggerFactory

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
abstract class FlowCommandBehavior : AbstractBpmnActivityBehavior() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    final override fun execute(execution: ActivityExecution) {
        val command = command(execution);
        command.flowId = execution.id
        logger.info("Flow queuing ${command.json}")
        command.async()
        execution.setVariable(command.name, JSON(command.json))
    }

    final override fun signal(execution: ActivityExecution, signalName: String?, signalData: Any?) {
        leave(execution)
    }

    protected fun <C: Command> command(type: Class<C>, execution: ActivityExecution): C {
        val name = type.newInstance().name
        val json = execution.getVariable(name) as SpinJsonNode
        return Command.toCommand(json.toString(), type)
    }

    protected fun <E: Event> event(type: Class<E>, execution: ActivityExecution): E {
        val name = type.newInstance().name
        val json = execution.getVariable(name) as SpinJsonNode
        return Event.toEvent(json.toString(), type)
    }

    abstract fun command(execution: ActivityExecution): Command

}
