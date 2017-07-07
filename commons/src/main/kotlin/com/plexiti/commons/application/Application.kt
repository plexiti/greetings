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
            val correlatesToFlow = correlate(event)
            event.internals().consume()
            if (!correlatesToFlow)
                event.internals().process()
        }
    }

    @Transactional
    fun handle(json: String) {
        val message = FlowIO.fromJson(json)
        Event.executingCommand.set(commandStore.findOne(message.flowId))
        when (message.type) {
            MessageType.Event -> {
                val event = Event.raise(message.event!!)
                event.internals().raisedBy = message.flowId
            }
            MessageType.Command -> {
                val command = Command.issue(message.command!!)
                command.internals().issuedBy = message.flowId
                command.internals().correlatedToToken = message.tokenId
            }
        }
        Event.executingCommand.set(null)
    }

    @Transactional
    fun execute(json: String): Any? {
        val commandId = commandStore.commandId(json)
        val command = commandStore.findOne(commandId) ?: commandStore.save(Command.fromJson(json))
        return execute(command)
    }

    @Transactional
    fun process(command: Command): Any? {
        return execute(Command.issue(command))
    }

    @Transactional
    fun execute(command: Command): Any? {
        Event.executingCommand.set(command)
        command.internals().start()
        try {
            return run(command)
        } catch (problem: Problem) {
            command.internals().correlate(problem)
            return problem
        } finally {
            Event.executingCommand.set(null)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private fun run(command: Command): Any? {
        try {
            val result = route.requestBody("direct:${command.name.name}", command)
            if (result is Value) {
                valueStore.save(result)
                command.internals().correlate(result)
                return result
            }
            return eventStore.findAll_OrderByRaisedAtDesc(command.internals().correlatedToEvents!!.toMutableList())
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

    private fun correlate(event: Event): Boolean {
        var correlatesToFlow = false
        Command.store.types.values.forEach {
             val instance = it.java.newInstance()
             val correlation = instance.correlation(event)
             if (correlation != null) {
                 val command = commandStore.findByCorrelatedBy_AndExecutionFinishedAt_IsNull(correlation)
                 if (command != null) {
                     command.internals().correlate(event)
                     if (command is Flow)
                         correlatesToFlow = true
                 }
             }
         }
         return correlatesToFlow
    }

}
