package com.plexiti.commons.application

import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.ValueStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.domain.Value
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.MessageType
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
class Application {

    @Autowired
    var commandStore: CommandStore = Command.store

    @Autowired
    var eventStore: EventStore = Event.store

    @Autowired
    var valueStore: ValueStore = Value.store

    @Autowired
    private lateinit var route: ProducerTemplate

    @Transactional
    fun consume(json: String) {
        val eventId = eventStore.eventId(json)
        if (eventId != null) {
            val event = eventStore.findOne(eventId) ?: eventStore.save(Event.fromJson(json))
            triggerBy(event)
            correlate(event)
            event.internals().consume()
        }
    }

    @Transactional
    fun handle(json: String) {
        val message = FlowIO.fromJson(json)
        Event.executingCommand.set(commandStore.findOne(message.flowId))
        when (message.type) {
            MessageType.Event -> {
                val event = Event.raise(message.event!!)
                event.internals().flowId = message.flowId
            }
            MessageType.Command -> {
                val command = Command.issue(message.command!!)
                command.internals().flowId = message.flowId
                command.internals().tokenId = message.tokenId
            }
        }
        Event.executingCommand.set(null)
    }

    @Transactional
    fun execute(json: String): Any? {
        val commandId = commandStore.commandId(json)
        if (commandId != null) {
            val command = commandStore.findOne(commandId) ?: commandStore.save(Command.fromJson(json))
            Event.executingCommand.set(command)
            command.internals().start()
            try {
                return execute(command)
            } catch (problem: Problem) {
                command.internals().finish(problem)
                return problem
            } finally {
                Event.executingCommand.set(null)
            }
        }
        throw IllegalArgumentException()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private fun execute(command: Command): Any? {
        try {
            val result = route.requestBody("direct:${command.name.name}", command)
            if (result is Value) {
                valueStore.save(result)
                command.internals().finish(result)
                return result
            }
            return Unit
        } catch (e: CamelExecutionException) {
            throw e.exchange.exception
        }
    }

    private fun triggerBy(event: Event) {
        Command.store.types.values.forEach {
            val instance = it.java.newInstance()
            var command = instance.trigger(event)
            if (command != null) {
                command = Command.issue(command)
                command.internals().triggeredBy = event.id
            }
        }
    }

    private fun correlate(event: Event) {
        Command.store.types.values.forEach {
             val instance = it.java.newInstance()
             val correlation = instance.correlation(event)
             if (correlation != null) {
                 val command = commandStore.findByCorrelationAndExecutionFinishedAtIsNull(correlation)
                 if (command != null) {
                     command.internals().finish(event)
                     event.internals().flowId = if (command is Flow) command.id else command.internals().flowId
                 }
             }
         }
    }

}
