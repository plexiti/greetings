package com.plexiti.greetings.adapters.flow

import com.plexiti.commons.adapters.flow.FlowEvent
import com.plexiti.greetings.domain.Greeting
import com.plexiti.greetings.domain.Greeting.CallerContactedPersonally
import com.plexiti.greetings.domain.GreetingId
import com.plexiti.greetings.domain.GreetingRepository
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class CallerContactedPersonallyEvent: FlowEvent<CallerContactedPersonally>() {

    @Autowired
    lateinit var greetingRepository: GreetingRepository

    override fun event(execution: DelegateExecution): CallerContactedPersonally {
        val event = event(Greeting.CallAnsweredAutomatically::class.java, execution)
        val greeting = greetingRepository.findOne(GreetingId(event.aggregate!!.id))
        return CallerContactedPersonally(greeting)
    }

}
