package com.plexiti.commons.adapters.flow

import com.plexiti.commons.application.CommandEntity
import com.plexiti.commons.domain.Event
import org.apache.camel.Handler
import org.apache.camel.builder.RouteBuilder
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.spin.json.SpinJsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Configuration
@Profile("prod")
class FlowCommandFinisher : RouteBuilder() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var flow: ProcessEngine

    val options = "consumer.namedQuery=CommandFinisher&consumeDelete=false"

    override fun configure() {
        from("jpa:${CommandEntity::class.qualifiedName}?${options}")
            .bean(this)
    }

    @Handler
    fun complete(command: CommandEntity) {

        val execution = flow.runtimeService
            .createExecutionQuery()
            .executionId(command.flowId)
            .singleResult();

        if (execution != null) {

            val event = Event.findOne(command.completedBy!!)!!
            flow.runtimeService.signal(execution.id, mapOf(event.name to SpinJsonNode.JSON(event.json)))

            logger.info("Flow forwarded ${command.json}")

        }

    }

}
