package com.plexiti.commons.application

import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.ValueStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.domain.*
import org.apache.camel.CamelExecutionException
import org.apache.camel.ProducerTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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
            val flowId = event.internals().raisedByFlow
            val flow = if (flowId != null) commandStore.findOne(event.internals().raisedByFlow)?.internals() as StoredFlow? else null
            flow?.resume()
            triggerBy(event)
            correlate(event)
            event.internals().consume()
            // TODO Events not raised by a flow, but correlated to it are just consumed
            event.internals().process()
            flow?.hibernate()
        }
    }

    @Transactional
    fun handle(json: String): FlowIO {
        val message = FlowIO.fromJson(json)
        val flow = commandStore.findOne(message.flowId)?.internals() as StoredFlow
        flow.resume()
        when (message.type) {
            MessageType.Event -> Event.raise(message)
            MessageType.Command -> Command.issue(message)
            else -> throw IllegalStateException()
        }
        flow.hibernate()
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
        c.internals().forward() // TODO semtantically wrong, but currently needed to silent the queuer
        return execute(c)
    }

    @Transactional
    fun execute(command: Command): Any? {
        command.internals().start()
        try {
            return run(command)
        } catch (problem: Problem) {
            command.internals().correlate(problem)
            return problem
        } finally {
            command.internals().finish()
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
            val events = command.internals().eventsAssociated?.keys?.toMutableList() ?: mutableListOf()
            return if (!events.isEmpty()) eventStore.findAll_OrderByRaisedAtDesc(events) else null
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
                command.internals().correlate(event)
            }
        }
    }

    private fun correlate(event: Event) {
        Command.store.types.values.forEach {
             val instance = it.java.newInstance()
             val correlation = instance.correlation(event)
             if (correlation != null) {
                 val command = commandStore.findByCorrelatedBy_AndExecutionFinishedAt_IsNull(correlation)
                 if (command != null) {
                     command.internals().correlate(event)
                     if (command !is Flow) command.internals().finish()
                 }
             }
         }
    }

}
