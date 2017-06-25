package com.plexiti.commons.application

import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.domain.Event
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
class CorrelationService {

    @Autowired
    lateinit var commandStore: CommandStore

    @Autowired
    lateinit var eventStore: EventStore

    @Transactional
    fun consumeEvent(json: String) {
        val eventId = eventStore.eventId(json)
        if (eventId != null) {
            val event = eventStore.findOne(eventId) ?: eventStore.save(Event.fromJson(json))
            triggerBy(event)
            finishBy(event)
            event.internals.transitioned()
        }
    }

    @Transactional
    fun executeCommand(json: String) {
        val commandId = commandStore.commandId(json)
        if (commandId != null) {
            val command = commandStore.findOne(commandId) ?: commandStore.save(Command.fromJson(json))
            command.internals.transitioned()
            Event.executingCommand.set(command)
            // TODO execute command
            Event.executingCommand.set(null)
        }
    }

    private fun triggerBy(event: Event) {
        commandStore.commandTypes.values.forEach {
            val instance = it.java.newInstance()
            var command = instance.triggerBy(event)
            if (command != null) {
                command = Command.issue(command)
                command.internals.triggeredBy = event.id
            }
        }
    }

    private fun finishBy(event: Event) {
         commandStore.commandTypes.values.forEach {
             val instance = it.java.newInstance()
             val finishKey = instance.finishKey(event)
             if (finishKey != null) {
                 val command = commandStore.findByFinishKey(finishKey)
                 if (command != null) {
                     command.internals.finishedBy = event.id
                     command.internals.transitioned()
                 }
             }
         }
    }

}
