package com.plexiti.commons.application

import com.plexiti.commons.DataJpaIntegration
import com.plexiti.commons.adapters.db.CommandStore
import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.adapters.db.ValueStore
import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
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
    lateinit var eventStore: EventStore

    @Autowired
    lateinit var commandStore: CommandStore

    @Autowired
    lateinit var valueStore: ValueStore

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

    @Before
    fun init() {
        eventStore.deleteAll()
        commandStore.deleteAll()
        valueStore.deleteAll()
    }

    class FlowITCommand(): Command() {

        var someCommandProperty: String? = null
        var someEventProperty: String? = null

        override fun init() {
            someCommandProperty = "someCommandValue"
            someEventProperty = get(FlowITEvent::class)?.someEventProperty
        }

    }

    class FlowITEvent(): Event() {

        var someEventProperty: String? = null
        var someCommandProperty: String? = null

        override fun init() {
            someEventProperty = "someEventValue"
            someCommandProperty = get(FlowITCommand::class)?.someCommandProperty
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

        val event = eventStore.findAll().iterator().next()

        assertThat(event.internals().status).isEqualTo(EventStatus.processed)
        assertThat(event.internals().raisedAt).isNotNull()
        assertThat(event.internals().forwardedAt).isNotNull()
        assertThat(event.internals().consumedAt).isNotNull()

        val command = commandStore.findAll().iterator().next()

        assertThat(command.internals().status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals().issuedAt).isNotNull()
        assertThat(command.internals().forwardedAt).isNull()
        assertThat(command.internals().getTriggeredBy()).isEqualTo(event.id)

    }

    @Test
    fun consumeEvent_internal() {

        Event.raise(InternalITEvent(aggregate))
        val event = eventStore.findAll().iterator().next()
        event.internals().forward()

        application.consume(event.toJson())

        assertThat(event.internals().status).isEqualTo(EventStatus.processed)
        assertThat(event.internals().raisedAt).isNotNull()
        assertThat(event.internals().forwardedAt).isNotNull()
        assertThat(event.internals().consumedAt).isNotNull()

        assertThat(commandStore.findAll()).isEmpty()

    }

    @Test
    fun executeCommand_internal() {

        val external = ExternalITEvent(aggregate)
        application.consume(external.toJson())

        val event = eventStore.findAll().iterator().next()

        assertThat(event.internals().status).isEqualTo(EventStatus.processed)
        assertThat(event.internals().raisedAt).isNotNull()
        assertThat(event.internals().forwardedAt).isNotNull()
        assertThat(event.internals().consumedAt).isNotNull()

        var command = commandStore.findAll().iterator().next()

        assertThat(command.internals().status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals().issuedAt).isNotNull()
        assertThat(command.internals().forwardedAt).isNull()
        assertThat(command.internals().getTriggeredBy()).isEqualTo(event.id)

        command.internals().forward()
        application.execute(command.toJson())

        val events = eventStore.findAll()

        assertThat(events).hasSize(2)
        assertThat(events.find { it.name.name ==  "ExternalITEvent" }).isNotNull()
        assertThat(events.find { it.name.name ==  "InternalITEvent" }).isNotNull()

        command = commandStore.findAll().iterator().next()

        assertThat(command.internals().status).isEqualTo(CommandStatus.processed)
        assertThat(command.internals().execution?.startedAt).isNotNull()
        assertThat(command.internals().execution?.finishedAt).isNotNull()

    }

    @Test
    fun executeCommand_external() {

        val command = Command.issue(ExternalITCommand())
        command.internals().forward()

        val event = ExternalITEvent(aggregate)

        application.consume(event.toJson())

        assertThat(command.internals().status).isEqualTo(CommandStatus.processed)
        assertThat(command.internals().execution?.startedAt).isNull()
        assertThat(command.internals().execution?.finishedAt).isNotNull()

    }

    @Test
    fun executeCommand_internal_withProblem() {

        val command = Command.issue(ProblemITCommand())
        command.internals().forward()
        application.execute(command.toJson())

        assertThat(command.internals().status).isEqualTo(CommandStatus.processed)
        assertThat(command.internals().execution?.finishedAt).isNotNull()
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
        assertThat(command.internals().execution?.finishedAt).isNotNull()
        assertThat(command.internals().problemOccured?.code).isNull()
        assertThat(command.internals().eventsAssociated).isNull()

    }

    @Test
    fun handleFlowIOCommand() {

        application.init(setOf(Name(name = "aFlow")))
        val flow = Command.issue(Flow(Name(name = "aFlow")))

        val eventMessage = FlowIO(Event(Name(name = "FlowITEvent")), flow.id)
        application.handle(eventMessage.toJson())

        val event = eventStore.findAll().first() as FlowITEvent

        val commandMessage = FlowIO(Command(Name(name = "FlowITCommand")), flow.id, TokenId("aTokenId"))

        application.handle(commandMessage.toJson())

        val command = commandStore.findAll().first { it !is Flow }  as FlowITCommand

        assertThat(event.someEventProperty).isEqualTo("someEventValue")
        assertThat(command.someCommandProperty).isEqualTo("someCommandValue")
        assertThat(command.someEventProperty).isEqualTo("someEventValue")

        assertThat(command.internals().status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals().issuedBy).isEqualTo(flow.id)
        assertThat(command.internals().executedBy).isEqualTo(TokenId("aTokenId"))
        assertThat(command.internals().eventsAssociated).isNull()

    }

    @Test
    fun handleFlowIOEvent() {

        application.init(setOf(Name(name = "aFlow")))
        val flow = Command.issue(Flow(Name(name = "aFlow")))

        val commandMessage = FlowIO(Command(Name(name = "FlowITCommand")), flow.id, TokenId())
        application.handle(commandMessage.toJson())

        val command = commandStore.findAll().first { it !is Flow } as FlowITCommand

        val eventMessage = FlowIO(Event(Name(name = "FlowITEvent")), command.internals().issuedBy!!)
        application.handle(eventMessage.toJson())
        val event = eventStore.findAll().first() as FlowITEvent

        assertThat(command.someCommandProperty).isEqualTo("someCommandValue")
        assertThat(event.someEventProperty).isEqualTo("someEventValue")
        assertThat(event.someCommandProperty).isEqualTo("someCommandValue")

        assertThat(event.internals().status).isEqualTo(EventStatus.raised)
        assertThat(event.internals().raisedByFlow).isEqualTo(flow.id)

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
