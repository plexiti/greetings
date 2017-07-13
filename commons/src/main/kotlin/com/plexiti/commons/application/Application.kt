package com.plexiti.commons.application

import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.ValueStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.domain.*
import com.plexiti.utils.scanPackageForClassNames
import com.plexiti.utils.scanPackageForNamedClasses
import org.apache.camel.*
import org.apache.camel.spring.SpringRouteBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
class Application: SpringRouteBuilder(), ApplicationContextAware {

    private val logger = LoggerFactory.getLogger("com.plexiti.commons.application")

    @org.springframework.beans.factory.annotation.Value("\${com.plexiti.app.context}")
    private var context = Name.context

    @Autowired
    var commandStore: CommandStore = Command.store

    @Autowired
    var eventStore: EventStore = Event.store

    @Autowired
    var commandRunner: CommandRunner = CommandRunner()

    init { init() }

    fun init(flows: Set<Name> = emptySet()) {
        Event.types = scanPackageForNamedClasses("com.plexiti", Event::class)
        Event.names = scanPackageForClassNames("com.plexiti", Event::class)
        Command.types = scanPackageForNamedClasses("com.plexiti", Command::class)
        Command.names = scanPackageForClassNames("com.plexiti", Command::class)
        flows.forEach { flowName ->
            Command.types = Command.types.plus(flowName to Flow::class)
        }
    }

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Event.store = eventStore
        Command.store = commandStore
        Name.context = context
    }

    @Transactional
    fun consume(json: String) {
        val incoming = Event.fromJson(json)
        val event = if (incoming.name.context == Name.context) eventStore.findOne(incoming.id) else commandRunner.receive(incoming)
        if (event != null) {
            logger.debug("Consuming ${event.toJson()}")
            val flowId = event.internals().raisedByFlow
            val flow = if (flowId != null) commandStore.findOne(event.internals().raisedByFlow)?.internals() as StoredFlow? else null
            flow?.resume()
            triggerBy(event)
            correlate(event)
            event.internals().consume()
            // TODO Events not raised by a flow, but correlated to it are just consumed
            event.internals().process()
            flow?.hibernate()
        } else {
            throw IllegalStateException()
        }
    }

    @Transactional
    fun handle(json: String): FlowIO {
        val message = FlowIO.fromJson(json)
        val flow = commandStore.findOne(message.flowId)!!.internals() as StoredFlow
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
        val incoming = Command.fromJson(json)
        val command = if (incoming.name.context == Name.context) commandStore.findOne(incoming.id) else commandRunner.receive(incoming)
        if (command != null) {
            return run(command)
        } else {
            throw IllegalStateException()
        }
    }

    @Transactional
    fun execute(command: Command): Any? {
        val command = commandRunner.issue(command)
        return run(command)
    }

    @Transactional
    fun run(command: Command): Any? {
        logger.debug("Executing ${command.toJson()}")
        val entity = command.internals()
        try {
            entity.start()
            return commandRunner.run(command)
        } catch (problem: Problem) {
            entity.correlate(problem)
            logger.info("Throwing ${problem.toJson()} for ${command.toJson()}")
            return problem
        } finally {
            entity.finish()
        }
    }

    @Component
    class CommandRunner {

        private val logger = LoggerFactory.getLogger("com.plexiti.commons.application")

        @Autowired
        private lateinit var route: ProducerTemplate

        @Autowired
        var eventStore: EventStore = Event.store

        @Autowired
        var valueStore: ValueStore = Value.store

        @Transactional (propagation = Propagation.REQUIRES_NEW)
        internal fun receive(event: Event): Event {
            val e = Event.receive(event)
            e.internals().forward() // TODO semantically wrong, but currently needed to silent the queuer
            return e;
        }

        @Transactional (propagation = Propagation.REQUIRES_NEW)
        internal fun receive(command: Command): Command {
            val c = Command.receive(command)
            c.internals().forward() // TODO semantically wrong, but currently needed to silent the queuer
            return c;
        }

        @Transactional (propagation = Propagation.REQUIRES_NEW)
        internal fun issue(command: Command): Command {
            val c = Command.issue(command)
            c.internals().forward() // TODO semantically wrong, but currently needed to silent the queuer
            return c
        }

        @Transactional (propagation = Propagation.REQUIRES_NEW)
        internal fun run(command: Command): Any? {
            try {
                val result = route.requestBody("direct:${command.name.name}", command)
                if (result is Value) {
                    valueStore.save(result)
                    command.internals().correlate(result)
                    logger.info("Returning ${result.toJson()} for ${command.toJson()}")
                    return result
                } else {
                    return Command.getRaised()
                }
            } catch (e: CamelExecutionException) {
                throw e.exchange.exception
            }
        }

    }
    
    private fun triggerBy(event: Event) {
        Command.types.forEach { name, type ->
            val instance = type.java.newInstance()
            instance.name = name // necessary for flows
            var command = instance.trigger(event)
            logger.debug("Investigating: is command '${instance.name}' triggered by ${event.toJson()}? ${if (command != null) "Yes" else "No"}.")
            if (command != null) {
                command = Command.issue(command)
                command.internals().correlate(event)
            }
        }
    }

    private fun correlate(event: Event) {
        Command.types.forEach { name, type ->
             val instance = type.java.newInstance()
             instance.name = name // necessary for flows
             val correlation = instance.correlation(event)
             if (correlation != null) {
                 val command = commandStore.findByCorrelatedBy_AndExecutionFinishedAt_IsNull(correlation)
                 logger.debug("Investigating: is ${event.toJson()} correlated to command '${instance.name}'? ${if (command != null) "Yes" else "No"}.")
                 if (command != null) {
                     command.internals().correlate(event)
                     if (command !is Flow) command.internals().finish()
                 }
             }
         }
    }

    override fun configure() {

        Command.types.entries.filter { it.key.context == Name.context } .forEach {

            val commandName = it.key.name
            val methodName = commandName.substring(0, 1).toLowerCase() + commandName.substring(1)
            val className = it.value.qualifiedName!!
            try {
                val bean = Class.forName(className.substring(0, className.length - methodName.length - 1))
                bean.getMethod(methodName, it.value.java)
                from("direct:${commandName}")
                    .bean(bean, methodName)
            } catch (n: NoSuchMethodException) {
                // TODO log
            } catch (c: ClassNotFoundException) {
                // TODO log
            }

        }
    }

}
