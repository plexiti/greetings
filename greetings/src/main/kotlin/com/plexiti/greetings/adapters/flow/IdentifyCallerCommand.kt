package com.plexiti.greetings.adapters.flow

import com.plexiti.commons.adapters.flow.FlowCommand
import com.plexiti.greetings.application.GreetingApplication.IdentifyCaller
import com.plexiti.greetings.domain.Greeting
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class IdentifyCallerCommand: FlowCommand() {

    override fun command(execution: ActivityExecution): IdentifyCaller {
        val event = event(Greeting.CallAnsweredAutomatically::class.java, execution)
        return IdentifyCaller(event.greeting!!)
    }

}
