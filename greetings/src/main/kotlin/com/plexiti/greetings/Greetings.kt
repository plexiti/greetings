package com.plexiti.greetings

import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.application.Flow
import com.plexiti.commons.domain.Name
import org.camunda.bpm.application.PostDeploy
import org.camunda.bpm.application.ProcessApplication
import org.camunda.bpm.engine.impl.event.EventType
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity
import org.camunda.bpm.spring.boot.starter.SpringBootProcessApplication
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@ComponentScan(group)
@EntityScan(group)
@EnableJpaRepositories(group)
@EnableRabbit
@ProcessApplication(name)
class Greetings: SpringBootProcessApplication() {

    @Autowired
    lateinit var commandStore: CommandStore

    @Autowired
    lateinit var eventStore: EventStore

    companion object {

        private val logger = LoggerFactory.getLogger(Greetings::class.java)

        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(Greetings::class.java, *args)
        }

    }

    @PostDeploy
    fun init() {


        val definitions = processEngine.repositoryService.createProcessDefinitionQuery().list()
        commandStore.init(flows = definitions.mapTo(HashSet(), {
            com.plexiti.commons.domain.Name(name=it.key)
        }))
        Flow.triggers = eventStore.names.values.filter {
            processEngine.repositoryService.createProcessDefinitionQuery().messageEventSubscriptionName(it.qualified).singleResult() != null
        }.associate {
            it to Name(name = processEngine.repositoryService.createProcessDefinitionQuery().messageEventSubscriptionName(it.qualified).singleResult().key)
        }

    }

}

const val name = "greetings";
const val group = "com.plexiti";

