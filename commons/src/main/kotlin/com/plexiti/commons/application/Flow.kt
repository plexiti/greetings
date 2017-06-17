package com.plexiti.commons.application;

import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.MessageType
import org.apache.camel.Handler
import org.apache.camel.builder.RouteBuilder
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.spin.json.SpinJsonNode
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class Flow: RouteBuilder() {

    companion object {
        lateinit var flow: ProcessEngine
    }

    open class FlowCommand: Command()

    class Start: FlowCommand() {

        override fun isTriggeredBy(event: Event): Boolean {

            val definition = flow.repositoryService
                .createProcessDefinitionQuery()
                .messageEventSubscriptionName(event.name)
                .singleResult();

            if (definition != null) {
                type = MessageType.Flow
                name = "${definition.key.substring(0, 1).toLowerCase()}${definition.key.substring(1)}"
                return true
            } else {
                return false
            }

        }

    }

    fun start(command: Start) {

        val event = Event.findOne(command.triggeredBy!!)!!

        flow.runtimeService
            .createMessageCorrelation(event.name)
            .setVariable(event.name, SpinJsonNode.JSON(event.json))
            .correlateStartMessage();

    }

    class Complete(): FlowCommand() {

        override fun isTriggeredBy(event: Event): Boolean {

            val command = Command.findOne(event.commandId!!)!!

            return command.flowId != null &&
                flow.runtimeService
                    .createExecutionQuery()
                    .executionId(command.flowId)
                    .singleResult() != null;

        }

    }

    fun complete(command: Complete) {

        val event = Event.findOne(command.triggeredBy!!)!!
        val command = Command.findOne(event.commandId!!)!!

        val execution = flow.runtimeService
            .createExecutionQuery()
            .executionId(command.flowId)
            .singleResult();

        if (execution != null) {
            flow.runtimeService
                .signal(execution.id, mapOf(event.name to SpinJsonNode.JSON(event.json)))
        }

    }

    override fun configure() {

        flow.repositoryService.createProcessDefinitionQuery().latestVersion().list().forEach {
            from("direct:${it.key.substring(0,1).toLowerCase()}${it.key.substring(1)}").bean(object {
                @Handler
                fun handle(c: Command): Start {
                    return Command.toCommand(c.json, Start::class.java)
                }
            }).bean(this::class.java, "start")
            Command.register(Start::class.java as Class<Command>)
        }
        from("direct:complete").bean(object {
            @Handler
            fun handle(c: Command): Complete {
                return Command.toCommand(c.json, Complete::class.java)
            }
        }).bean(this::class.java, "complete")
        Command.register(Complete::class.java as Class<Command>)

    }

}

@Component
private class FlowInitialiser : ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Flow.flow = applicationContext!!.getBean(ProcessEngine::class.java)
    }

}

