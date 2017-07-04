package com.plexiti.commons.application

import com.plexiti.commons.adapters.db.CommandRepository
import com.plexiti.commons.adapters.db.DocumentRepository
import com.plexiti.commons.adapters.db.EventRepository
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Problem
import org.apache.camel.CamelExecutionException
import org.apache.camel.ProducerTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
class ApplicationService {

    @Autowired
    var commandRepository: CommandRepository = Command.repository

    @Autowired
    var eventRepository: EventRepository = Event.repository

    @Autowired
    var documentRepository: DocumentRepository = Document.repository

    @Autowired
    private lateinit var route: ProducerTemplate

    @Transactional
    fun consumeEvent(json: String) {
        val eventId = eventRepository.eventId(json)
        if (eventId != null) {
            val event = eventRepository.findOne(eventId) ?: eventRepository.save(Event.fromJson(json))
            triggerCommand(event)
            finishCommand(event)
            event.internals().consume()
        }
    }

    @Transactional
    fun executeCommand(json: String) {
        val commandId = commandRepository.commandId(json)
        if (commandId != null) {
            val command = commandRepository.findOne(commandId) ?: commandRepository.save(Command.fromJson(json))
            Event.executingCommand.set(command)
            command.internals().start()
            try {
                executeCommand(command)
            } catch (problem: Problem) {
                command.internals().finish(problem)
            }
            Event.executingCommand.set(null)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private fun executeCommand(command: Command) {
        try {
            val result = route.requestBody("direct:${command.name.name}", command)
            if (result is Document) {
                documentRepository.save(result)
                command.internals().finish(result)
            }
        } catch (e: CamelExecutionException) {
            throw e.exchange.exception
        }
    }

    private fun triggerCommand(event: Event) {
        Command.types.values.forEach {
            val instance = it.java.newInstance()
            var command = instance.trigger(event)
            if (command != null) {
                command = Command.issue(command)
                command.internals().triggeredBy = event.id
            }
        }
    }

    private fun finishCommand(event: Event) {
        Command.types.values.forEach {
             val instance = it.java.newInstance()
             val finishKey = instance.correlation(event)
             if (finishKey != null) {
                 val command = commandRepository.findByCorrelationAndExecutionFinishedAtIsNull(finishKey)
                 if (command != null) {
                     command.internals().finish(event)
                 }
             }
         }
    }

}
