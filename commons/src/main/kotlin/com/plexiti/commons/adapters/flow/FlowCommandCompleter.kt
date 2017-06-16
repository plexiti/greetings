package com.plexiti.commons.adapters.flow

import com.plexiti.commons.domain.Event
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.spin.json.SpinJsonNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class FlowCommandCompleter {

    @Autowired
    lateinit var flow: ProcessEngine

    fun handle(event: Event) {

        val commandExecution = flow.runtimeService
            .createExecutionQuery()
            .variableValueEquals("commandId", event.commandId)
            .singleResult();

        if (commandExecution != null) {
            flow.runtimeService
                .signal(commandExecution.id, mapOf(event.name to SpinJsonNode.JSON(event.json)))
        }

    }

}
