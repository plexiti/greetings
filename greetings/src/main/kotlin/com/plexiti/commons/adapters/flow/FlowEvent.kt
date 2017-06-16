package com.plexiti.commons.adapters.flow

import com.plexiti.commons.application.Command
import com.plexiti.commons.domain.Event
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.spin.json.SpinJsonNode.JSON
import org.camunda.spin.json.SpinJsonNode

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
abstract class FlowEvent<out E: Event>: JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        val event = event(execution);
        Event.raise(event)
        execution.setVariable(event.name, JSON(event.json))
    }

    internal fun <C: Command<*>> command(type: Class<C>, execution: DelegateExecution): C {
        val name = type.newInstance().name
        val json = execution.getVariable(name) as SpinJsonNode
        return Command.toCommand(json.toString(), type)
    }

    internal fun <E: Event> event(type: Class<E>, execution: DelegateExecution): E {
        val name = type.newInstance().name
        val json = execution.getVariable(name) as SpinJsonNode
        return Event.toEvent(json.toString(), type)
    }

    abstract fun event(execution: DelegateExecution): E

}
