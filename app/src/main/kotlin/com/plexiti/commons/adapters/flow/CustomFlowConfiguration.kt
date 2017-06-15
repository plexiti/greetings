package com.plexiti.commons.adapters.flow

import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.persistence.StrongUuidGenerator
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.camunda.bpm.spring.boot.starter.configuration.Ordering
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Order(Ordering.DEFAULT_ORDER + 1)
class CustomFlowConfiguration : AbstractCamundaConfiguration() {

    @Value("\${org.camunda.bpm.configuration.databaseSchemaUpdate}")
    lateinit var databaseSchemaUpdate: String

    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        processEngineConfiguration.databaseSchemaUpdate = databaseSchemaUpdate
        processEngineConfiguration.isJobExecutorActivate = true
        processEngineConfiguration.isJobExecutorDeploymentAware = true
        processEngineConfiguration.history = ProcessEngineConfiguration.HISTORY_FULL
        processEngineConfiguration.processEnginePlugins = listOf(SpinProcessEnginePlugin())
        processEngineConfiguration.idGenerator = StrongUuidGenerator()
        processEngineConfiguration.customPostBPMNParseListeners = listOf(object: AbstractBpmnParseListener() {

            override fun parseUserTask(taskElement: Element?, scope: ScopeImpl?, activity: ActivityImpl?) {
                activity!!.setAsyncAfter(true, true)
            }

        })
    }

}
