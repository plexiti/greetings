package com.plexiti.greetings.adapters.flow

import com.plexiti.commons.adapters.flow.CommandIssuer
import com.plexiti.greetings.application.GreetingApplication
import com.plexiti.greetings.domain.Greeting
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class IdentifyCallerIssuer: CommandIssuer<GreetingApplication.IdentifyCaller>() {

    override fun command(execution: ActivityExecution): GreetingApplication.IdentifyCaller {
        val event = event(Greeting.CallAnsweredAutomatically::class.java, execution)
        return GreetingApplication.IdentifyCaller(event.greeting!!)
    }

}
