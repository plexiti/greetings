package com.plexiti.commons.adapters.flow

import com.plexiti.commons.domain.Event
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.impl.event.EventType
import org.camunda.spin.json.SpinJsonNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class FlowStartMessageHandler {

    @Autowired
    lateinit var flow: ProcessEngine

    fun handle(event: Event) {
        val eventSubscriptions = flow.runtimeService
            .createEventSubscriptionQuery()
            .eventType(EventType.MESSAGE.name())
            .eventName(event.name)
            .count();

        if (eventSubscriptions > 0) {
            flow.runtimeService
                .createMessageCorrelation(event.name)
                .setVariable(event.name, SpinJsonNode.JSON(event.json))
                .correlateStartMessage();
        }

    }

}
