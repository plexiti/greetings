package com.plexiti.greetings.application;

import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.application.Command
import com.plexiti.commons.domain.EventStatus
import com.plexiti.commons.domain.Value
import com.plexiti.greetings.domain.Greeting
import com.plexiti.greetings.domain.Greeting.CallAnsweredAutomatically
import com.plexiti.greetings.domain.GreetingRepository
import com.plexiti.greetings.domain.GreetingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
@Transactional
class GreetingApplication {

    @Autowired
    lateinit var greetingService: GreetingService

    @Autowired
    lateinit var greetingRepository: GreetingRepository

    // A command object
    class AnswerCaller(var caller: String? = null): Command()

    // The execution of a command
    // may through a (business) problem which will be dealt with in flow
    fun answerCaller(command: AnswerCaller) {
        val caller = command.caller
        if (caller != null) greetingService.answer(caller)
    }

    // Another command
    class IdentifyCaller(var caller: String? = null): Command() {

        // callback to construct object out of flow data
        // works conceptually, but I need a few helper methods
        override fun construct() {
            caller = "Bernd" // at the moment, we just know Bernd :-) as soon as he called once
        }

    }

    // Again, the execution of a command
    fun identifyCaller(command: IdentifyCaller): CallerStatus {
        return CallerStatus(greetingRepository.findByCaller(command.caller)?.isKnown())
    }

    // A projection or "document". May be returned by a command
    // and will be passed on the flow.
    data class CallerStatus(val known: Boolean? = false): Value

}
