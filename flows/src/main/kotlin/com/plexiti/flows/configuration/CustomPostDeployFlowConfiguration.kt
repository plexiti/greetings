package com.plexiti.flows.configuration

import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.application.Flow
import com.plexiti.commons.domain.Name
import org.camunda.bpm.engine.ProcessEngine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class CustomPostDeployFlowConfiguration {

    @Autowired
    lateinit var processEngine: ProcessEngine

    @Autowired
    lateinit var commandStore: CommandStore

    @Autowired
    lateinit var eventStore: EventStore

    fun init() {

        val definitions = processEngine.repositoryService.createProcessDefinitionQuery().list()
        commandStore.init(flows = definitions.mapTo(HashSet(), {
            com.plexiti.commons.domain.Name(name=it.key)
        }))
        Flow.triggers = eventStore.names.values.filter {
            processEngine.repositoryService.createProcessDefinitionQuery()
                .messageEventSubscriptionName(it.qualified)
                .latestVersion().singleResult() != null
        }.associate {
            it to Name(name = processEngine.repositoryService
                .createProcessDefinitionQuery().messageEventSubscriptionName(it.qualified)
                .latestVersion().singleResult().key)
        }

        val triggers = Flow.triggers
        return

    }

}
