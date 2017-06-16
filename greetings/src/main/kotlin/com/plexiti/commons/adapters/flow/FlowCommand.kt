package com.plexiti.commons.adapters.flow

import com.plexiti.commons.application.Command
import com.plexiti.commons.domain.Event
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.spin.json.SpinJsonNode.JSON
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.camunda.spin.json.SpinJsonNode

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
abstract class FlowCommand<out C: Command<*>>: AbstractBpmnActivityBehavior() {

    final override fun execute(execution: ActivityExecution) {
        val command = command(execution);
        execution.setVariableLocal("commandId", command.id)
        Command.issue(command as Command<Unit>)
        execution.setVariable(command.name, JSON(command.json))
    }

    final override fun signal(execution: ActivityExecution, signalName: String?, signalData: Any?) {
        execution.removeVariableLocal("commandId")
        leave(execution)
    }

    internal fun <C: Command<*>> command(type: Class<C>, execution: DelegateExecution): C {
        val name = type.newInstance().name
        val json = execution.getVariable(name) as SpinJsonNode
        return Command.toCommand(json.toString(), type)
    }

    internal fun <E: Event> event(type: Class<E>, execution: ActivityExecution): E {
        val name = type.newInstance().name
        val json = execution.getVariable(name) as SpinJsonNode
        return Event.toEvent(json.toString(), type)
    }

    abstract fun command(execution: ActivityExecution): C

}
