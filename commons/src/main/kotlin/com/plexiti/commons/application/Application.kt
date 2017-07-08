package com.plexiti.commons.application

import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.ValueStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.domain.*
import org.apache.camel.CamelExecutionException
import org.apache.camel.ProducerTemplate
import org.slf4j.LoggerFactory
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
            // if (!correlatesToFlow) Todo Events raised by the process are processed, events to be correlated to the
            // process are just consumed
                event.internals().process()
        }
    }

    @Transactional
    fun handle(json: String): FlowIO {
        val message = FlowIO.fromJson(json)
        Event.executingCommand.set(commandStore.findOne(message.flowId))
        when (message.type) {
            MessageType.Event -> Event.raise(message)
            MessageType.Command -> Command.issue(message)
        }
        Event.executingCommand.set(null)
        return message
    }

    @Transactional
    fun execute(json: String): Any? {
        val commandId = commandStore.commandId(json)
        val command = commandStore.findOne(commandId) ?: commandStore.save(Command.fromJson(json))
        return execute(command)
    }

    @Transactional
    fun synchronous(command: Command): Any? {
        val c = Command.issue(command)
        c.internals().forward()
        return execute(c)
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

    @Transactional // (propagation = Propagation.REQUIRES_NEW)
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
        Command.store.types.forEach { name, type ->
            val instance = type.java.newInstance()
            instance.name = name // necessary for flows
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
