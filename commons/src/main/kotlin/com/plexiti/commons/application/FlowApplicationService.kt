package com.plexiti.commons.application;

import com.plexiti.commons.domain.Event
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.impl.event.EventType
import org.camunda.spin.json.SpinJsonNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.annotation.PostConstruct

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
@Transactional
class FlowApplicationService: CommandExecutor() {

    @Autowired
    lateinit var flow: ProcessEngine

    companion object {
        lateinit var flow: ProcessEngine
    }

    @PostConstruct
    fun init() {
        FlowApplicationService.flow = this.flow
    }

    class StartFlow(): Command<Unit>() {

        override fun isTriggeredBy(event: Event): Boolean {

            val startEventNames = flow.runtimeService
                .createEventSubscriptionQuery()
                .eventType(EventType.MESSAGE.name())
                .list().map { it.eventName };

            return startEventNames.contains(event.name)
        }

    }

    fun startFlow(command: StartFlow) {
        val event = Event.findOne(command.triggeredBy!!)!!
        flow.runtimeService
            .createMessageCorrelation(event.name)
            .setVariable(event.name, SpinJsonNode.JSON(event.json))
            .correlateStartMessage();
    }

    class CompleteActivity(): Command<Unit>() {

        override fun isTriggeredBy(event: Event): Boolean {

            return flow.runtimeService
                .createExecutionQuery()
                .variableValueEquals("commandId", event.commandId)
                .singleResult() != null;

        }

    }

    fun completeActivity(command: CompleteActivity) {

        val event = Event.findOne(command.triggeredBy!!)!!

        val execution = flow.runtimeService
            .createExecutionQuery()
            .variableValueEquals("commandId", event.commandId)
            .singleResult();

        if (execution != null) {
            flow.runtimeService
                .signal(execution.id, mapOf(event.name to SpinJsonNode.JSON(event.json)))
        }

    }

}
