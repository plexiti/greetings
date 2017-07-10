package com.plexiti.commons.application

import com.plexiti.commons.DataJpaIntegration
import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
open class ApplicationIntegration : DataJpaIntegration() {

    @Autowired
    lateinit var application: Application

    @Autowired
    lateinit var eventRepository: EventStore

    @Autowired
    lateinit var commandRepository: CommandStore

    lateinit var aggregate: ITAggregate

    class InternalITEvent(aggregate: ITAggregate? = null) : Event(aggregate)
    class ExternalITEvent(aggregate: ITAggregate? = null) : Event(aggregate) {
        override var name = Name("External_ExternalITEvent")
        val businessKey = "myCorrelationKey"
    }
    class ITAggregate: Aggregate<AggregateId>()
    class ITAggregateId(value: String = ""): AggregateId(value)

    class TriggeredITCommand(): Command() {
        override fun trigger(event: Event): Command? {
            return if (event.name.qualified.equals("External_ExternalITEvent")) TriggeredITCommand() else null
        }
    }

    class FlowITCommand(): Command() {

        var someCommandProperty: String? = null
        var someEventProperty: String? = null

        constructor(someCommandProperty: String): this() {
            this.someCommandProperty = someCommandProperty
        }

        override fun construct() {
            someCommandProperty = "someCommandValue"
            // someEventProperty = event(FlowITEvent::class)?.someEventProperty
        }

    }

    class FlowITEvent(): Event() {

        var someEventProperty: String? = null
        var someCommandProperty: String? = null

        constructor(someEventProperty: String): this() {
            this.someEventProperty = someEventProperty
        }

        override fun construct() {
            someEventProperty = "someEventValue"
            // someCommandProperty = command(FlowITCommand::class)?.someEventProperty
        }

    }

    class ProblemITCommand: Command()

    class ExceptionITCommand: Command()

    class QueryITCommand: Command()

    class QueryITResult: Value {
        val anything = "Something"
    }

    class ExternalITCommand: Command() {

        override var name = Name("External_ExternalITCommand")

        override fun correlation(): Correlation {
            return Correlation.create("myCorrelationKey")!!
        }

        override fun correlation(event: Event): Correlation? {
            if (event is ExternalITEvent) {
                return Correlation.create(event.businessKey)
            }
            return null
        }

    }

    @Before
    fun prepare() {
        aggregate = ITAggregate()
        aggregate.id = ITAggregateId(UUID.randomUUID().toString())
    }

    @Test
    fun consumeEvent_external() {

        val external = ExternalITEvent(aggregate)
        application.consume(external.toJson())

        val event = eventRepository.findAll().iterator().next()

        assertThat(event.internals().status).isEqualTo(EventStatus.processed)
        assertThat(event.internals().raisedAt).isNotNull()
        assertThat(event.internals().forwardedAt).isNull()
        assertThat(event.internals().consumedAt).isNotNull()

        val command = commandRepository.findAll().iterator().next()

        assertThat(command.internals().status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals().issuedAt).isNotNull()
        assertThat(command.internals().forwardedAt).isNull()
        assertThat(command.internals().getTriggeredBy()).isEqualTo(event.id)

    }

    @Test
    fun consumeEvent_internal() {

        Event.raise(InternalITEvent(aggregate))
        val event = eventRepository.findAll().iterator().next()
        event.internals().forward()

        application.consume(event.toJson())

        assertThat(event.internals().status).isEqualTo(EventStatus.processed)
        assertThat(event.internals().raisedAt).isNotNull()
        assertThat(event.internals().forwardedAt).isNotNull()
        assertThat(event.internals().consumedAt).isNotNull()

        assertThat(commandRepository.findAll()).isEmpty()

    }

    @Test
    fun executeCommand_internal() {

        val external = ExternalITEvent(aggregate)
        application.consume(external.toJson())

        val event = eventRepository.findAll().iterator().next()

        assertThat(event.internals().status).isEqualTo(EventStatus.processed)
        assertThat(event.internals().raisedAt).isNotNull()
        assertThat(event.internals().forwardedAt).isNull()
        assertThat(event.internals().consumedAt).isNotNull()

        var command = commandRepository.findAll().iterator().next()

        assertThat(command.internals().status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals().issuedAt).isNotNull()
        assertThat(command.internals().forwardedAt).isNull()
        assertThat(command.internals().getTriggeredBy()).isEqualTo(event.id)

        command.internals().forward()
        application.execute(command.toJson())

        val events = eventRepository.findAll()

        assertThat(events).hasSize(2)
        assertThat(events.find { it.name.name ==  "ExternalITEvent" }).isNotNull()
        assertThat(events.find { it.name.name ==  "InternalITEvent" }).isNotNull()

        command = commandRepository.findAll().iterator().next()

        assertThat(command.internals().status).isEqualTo(CommandStatus.processed)
        assertThat(command.internals().execution.startedAt).isNotNull()
        assertThat(command.internals().execution.finishedAt).isNotNull()

    }

    @Test
    fun executeCommand_external() {

        val command = Command.issue(ExternalITCommand())
        command.internals().forward()

        val event = ExternalITEvent(aggregate)

        application.consume(event.toJson())

        assertThat(command.internals().status).isEqualTo(CommandStatus.processed)
        assertThat(command.internals().execution.startedAt).isNull()
        assertThat(command.internals().execution.finishedAt).isNotNull()

    }

    @Test
    fun executeCommand_internal_withProblem() {

        val command = Command.issue(ProblemITCommand())
        command.internals().forward()
        application.execute(command.toJson())

        assertThat(command.internals().status).isEqualTo(CommandStatus.processed)
        assertThat(command.internals().execution.finishedAt).isNotNull()
        assertThat(command.internals().problemOccured?.code).isEqualTo(Problem::class.simpleName)

    }

    @Test(expected = RuntimeException::class)
    fun executeCommand_internal_withException() {

        val command = Command.issue(ExceptionITCommand())
        command.internals().forward()
        application.execute(command.toJson())

    }

    @Test
    fun executeCommand_internal_withResult() {

        val command = Command.issue(QueryITCommand())
        command.internals().forward()
        application.execute(command.toJson())

        assertThat(command.internals().status).isEqualTo(CommandStatus.processed)
        assertThat(command.internals().execution.finishedAt).isNotNull()
        assertThat(command.internals().problemOccured?.code).isNull()
        assertThat(command.internals().eventsAssociated).isNull()

    }

    @Test
    @Ignore // TODO
    fun handleFlowIOCommand() {

        Command.store.init(setOf(Name(name = "aFlow")))
        val flow = Command.issue(Flow(Name(name = "aFlow")))
        val event = FlowIO(FlowITEvent("someEventValue"), flow.id)
        
        application.handle(event.toJson())

        val flowCommand = Command(Name(name = "FlowITCommand"))
        val message = FlowIO(flowCommand, flow.id, TokenId("aTokenId"))

        application.handle(message.toJson())

        val command = commandRepository.findOne(flowCommand.id) as FlowITCommand

        assertThat(command.someCommandProperty).isEqualTo("someCommandValue")
        assertThat(command.someEventProperty).isEqualTo("someEventValue")

        assertThat(command.internals().status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals().issuedBy).isEqualTo(flow.id)
        assertThat(command.internals().executedBy).isEqualTo(TokenId("aTokenId"))
        assertThat(command.internals().eventsAssociated).isNull()

    }

    @Test
    @Ignore // TODO
    fun handleFlowIOEvent() {

        Command.store.init(setOf(Name(name = "aFlow")))
        val flow = Command.issue(Flow(Name(name = "aFlow")))

        val command = FlowIO(FlowITCommand("someCommandValue"), flow.id, TokenId("aTokenId"))

        application.handle(command.toJson())

        val flowEvent = Event(Name(name = "FlowITEvent"))
        val message = FlowIO(flowEvent, flow.id)
        application.handle(message.toJson())

        val event = eventRepository.findOne(flowEvent.id) as FlowITEvent

        assertThat(event.someEventProperty).isEqualTo("someEventValue")
        assertThat(event.someCommandProperty).isEqualTo("someCommandValue")

        assertThat(event.internals().status).isEqualTo(EventStatus.raised)
        assertThat(event.internals().raisedByCommand).isEqualTo(flow.id)

    }

    fun triggeredITCommand(command: TriggeredITCommand) {
        prepare()
        Event.raise(InternalITEvent(aggregate))
    }

    fun problemITCommand(command: ProblemITCommand) {
        throw Problem()
    }

    fun exceptionITCommand(command: ExceptionITCommand) {
        throw RuntimeException()
    }

    fun queryITCommand(command: QueryITCommand): Value {
        return QueryITResult()
    }

}
