package com.plexiti.commons.application

import com.plexiti.commons.AbstractDataJpaTest
import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.RuntimeException

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
open class ApplicationServiceIT : AbstractDataJpaTest() {

    @Autowired
    lateinit var applicationService: ApplicationService

    lateinit var aggregate: ITAggregate

    class InternalITEvent(aggregate: ITAggregate? = null) : Event(aggregate)
    class ExternalITEvent(aggregate: ITAggregate? = null) : Event(aggregate) {
        override var name = Name("External/ExternalITEvent")
        val businessKey = "myCorrelationKey"
    }
    class ITAggregate: Aggregate<AggregateId>()
    class ITAggregateId(value: String = ""): AggregateId(value)

    class TriggeredITCommand(): Command() {
        override fun trigger(event: Event): Command? {
            return if (event.name.qualified.equals("External/ExternalITEvent")) TriggeredITCommand() else null
        }
    }

    class ProblemITCommand: Command()

    class ExceptionITCommand: Command()

    class QueryITCommand: Command()

    class ExternalITCommand: Command() {

        override var name = Name("External/ExternalITCommand")

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
        applicationService.consumeEvent(external.toJson())

        val event = Event.store.findAll().iterator().next()

        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNull()
        assertThat(event.internals.consumedAt).isNotNull()

        val command = Command.store.findAll().iterator().next()

        assertThat(command.internals.status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals.issuedAt).isNotNull()
        assertThat(command.internals.forwardedAt).isNull()
        assertThat(command.internals.triggeredBy).isEqualTo(event.id)

    }

    @Test
    fun consumeEvent_internal() {

        Event.raise(InternalITEvent(aggregate))
        val event = Event.store.findAll().iterator().next()
        event.internals.forward()

        applicationService.consumeEvent(event.toJson())

        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNotNull()
        assertThat(event.internals.consumedAt).isNotNull()

        assertThat(Command.store.findAll()).isEmpty()

    }

    @Test
    fun executeCommand_internal() {

        val external = ExternalITEvent(aggregate)
        applicationService.consumeEvent(external.toJson())

        val event = Event.store.findAll().iterator().next()

        assertThat(event.internals.status).isEqualTo(EventStatus.consumed)
        assertThat(event.internals.raisedAt).isNotNull()
        assertThat(event.internals.forwardedAt).isNull()
        assertThat(event.internals.consumedAt).isNotNull()

        var command = Command.store.findAll().iterator().next()

        assertThat(command.internals.status).isEqualTo(CommandStatus.issued)
        assertThat(command.internals.issuedAt).isNotNull()
        assertThat(command.internals.forwardedAt).isNull()
        assertThat(command.internals.triggeredBy).isEqualTo(event.id)

        command.internals.forward()
        applicationService.executeCommand(command.toJson())

        val events = Event.store.findAll()

        assertThat(events).hasSize(2)
        assertThat(events.find { it.name.name ==  "ExternalITEvent" }).isNotNull()
        assertThat(events.find { it.name.name ==  "InternalITEvent" }).isNotNull()

        command = Command.store.findAll().iterator().next()

        assertThat(command.internals.status).isEqualTo(CommandStatus.completed)
        assertThat(command.internals.execution.startedAt).isNotNull()
        assertThat(command.internals.execution.finishedAt).isNotNull()

    }

    @Test
    fun executeCommand_external() {

        val command = Command.issue(ExternalITCommand())
        command.internals.forward()

        val event = ExternalITEvent(aggregate)

        applicationService.consumeEvent(event.toJson())

        assertThat(command.internals.status).isEqualTo(CommandStatus.completed)
        assertThat(command.internals.execution.startedAt).isNull()
        assertThat(command.internals.execution.finishedAt).isNotNull()

    }

    @Test
    fun executeCommand_internal_withProblem() {

        val command = Command.issue(ProblemITCommand())
        command.internals.forward()
        applicationService.executeCommand(command.toJson())

        assertThat(command.internals.status).isEqualTo(CommandStatus.completed)
        assertThat(command.internals.execution.finishedAt).isNotNull()
        assertThat(command.internals.execution.returnCode).isEqualTo(Problem::class.simpleName)
        assertThat(command.internals.execution.problem()).isNotNull()

    }

    @Test(expected = RuntimeException::class)
    fun executeCommand_internal_withException() {

        val command = Command.issue(ExceptionITCommand())
        command.internals.forward()
        applicationService.executeCommand(command.toJson())

    }

    @Test
    fun executeCommand_internal_withResult() {

        val command = Command.issue(QueryITCommand())
        command.internals.forward()
        applicationService.executeCommand(command.toJson())

        assertThat(command.internals.status).isEqualTo(CommandStatus.completed)
        assertThat(command.internals.execution.finishedAt).isNotNull()
        assertThat(command.internals.execution.returnCode).isNull()
        assertThat(command.internals.execution.finishedBy).isNull()
        assertThat(command.internals.execution.json).isNotNull()

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

    fun queryITCommand(command: QueryITCommand): Any {
        return object {
            val anything = "Something"
        }
    }

}
