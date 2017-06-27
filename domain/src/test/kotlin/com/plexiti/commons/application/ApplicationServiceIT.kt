package com.plexiti.commons.application

import com.plexiti.commons.AbstractDataJpaTest
import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
open class ApplicationServiceIT : AbstractDataJpaTest() {

    @Autowired
    lateinit var applicationService: ApplicationService

    lateinit var aggregate: ITAggregate

    class InternalITEvent(aggregate: ITAggregate? = null) : Event(aggregate)
    class ExternalITEvent(aggregate: ITAggregate? = null) : Event(aggregate) {
        override var context = Context("External")
        val businessKey = "myCorrelationKey"
    }
    class ITAggregate: Aggregate<AggregateId>()
    class ITAggregateId(value: String = ""): AggregateId(value)

    class TriggeredITCommand(): Command() {
        override fun triggerBy(event: Event): Command? {
            return if (event.qname().equals("External/ExternalITEvent")) TriggeredITCommand() else null
        }
    }

    class ProblemITCommand: Command()

    class QueryITCommand: Command()

    class ExternalITCommand: Command() {

        override var context = Context("External")

        override fun finishKey(): CorrelationKey {
            return CorrelationKey.create("myCorrelationKey")!!
        }

        override fun finishKey(event: Event): CorrelationKey? {
            if (event is ExternalITEvent) {
                return CorrelationKey.create(event.businessKey)
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
        event.internals.transitioned()

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
        assertThat(events.find { it.name ==  "ExternalITEvent" }).isNotNull()
        assertThat(events.find { it.name ==  "InternalITEvent" }).isNotNull()

        command = Command.store.findAll().iterator().next()

        assertThat(command.internals.status).isEqualTo(CommandStatus.finished)
        assertThat(command.internals.execution.startedAt).isNotNull()
        assertThat(command.internals.execution.finishedAt).isNotNull()

    }

    @Test
    fun executeCommand_external() {

        val command = Command.issue(ExternalITCommand())
        command.internals.forward()

        val event = ExternalITEvent(aggregate)

        applicationService.consumeEvent(event.toJson())

        assertThat(command.internals.status).isEqualTo(CommandStatus.finished)
        assertThat(command.internals.execution.startedAt).isNull()
        assertThat(command.internals.execution.finishedAt).isNotNull()

    }

    @Test
    fun executeCommand_withProblem() {

        val command = Command.issue(ProblemITCommand())
        command.internals.forward()
        applicationService.executeCommand(command.toJson())

        assertThat(command.internals.status).isEqualTo(CommandStatus.finished)
        assertThat(command.internals.execution.finishedAt).isNotNull()
        assertThat(command.internals.execution.returnCode).isEqualTo(Problem::class.simpleName)
        assertThat(command.internals.execution.problem()).isNotNull()

    }

    @Test
    fun executeCommand_withResult() {

        val command = Command.issue(QueryITCommand())
        command.internals.forward()
        applicationService.executeCommand(command.toJson())

        assertThat(command.internals.status).isEqualTo(CommandStatus.finished)
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

    fun queryITCommand(command: QueryITCommand): Any {
        return object {
            val anything = "Something"
        }
    }

}
