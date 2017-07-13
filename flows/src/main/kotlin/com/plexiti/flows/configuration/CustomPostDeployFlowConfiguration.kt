package com.plexiti.flows.configuration

import com.plexiti.commons.application.Application
import com.plexiti.commons.application.Command
import com.plexiti.commons.application.Flow
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Name
import org.camunda.bpm.engine.ProcessEngine
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.reflect.full.isSubclassOf

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class CustomPostDeployFlowConfiguration {

    private val logger = LoggerFactory.getLogger("com.plexiti.flows.configuration")

    @Autowired
    lateinit var processEngine: ProcessEngine

    @Autowired
    lateinit var application: Application

    fun init() {

        val definitions = processEngine.repositoryService.createProcessDefinitionQuery().list()

        application.init(flows = definitions.mapTo(HashSet(), {
            com.plexiti.commons.domain.Name(name=it.key)
        }))

        Flow.triggers = Event.names.values.filter {
            processEngine.repositoryService.createProcessDefinitionQuery()
                .messageEventSubscriptionName(it.qualified)
                .latestVersion().singleResult() != null
        }.associate {
            it to Name(name = processEngine.repositoryService
                .createProcessDefinitionQuery().messageEventSubscriptionName(it.qualified)
                .latestVersion().singleResult().key)
        }

        Command.types.forEach { commandName, commandType ->
            if (commandType::class.isSubclassOf(Flow::class)) {
                logger.info("Registered flow ${commandName.qualified}")
            } else {
                logger.info("Registered command ${commandName.qualified} for type ${commandType.qualifiedName}")
            }
        }

        Event.types.forEach { eventName, eventType ->
            logger.info("Registered event ${eventName.qualified} for type ${eventType.qualifiedName}")
        }

        Flow.triggers.forEach { eventName, flowName ->
            logger.info("Listening for event ${eventName.qualified} to trigger ${flowName.qualified}")
        }

    }

}
