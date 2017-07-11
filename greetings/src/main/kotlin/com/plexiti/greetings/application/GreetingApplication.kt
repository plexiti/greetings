package com.plexiti.greetings.application;

import com.plexiti.commons.application.Command
import com.plexiti.commons.domain.Value
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
class GreetingApplication {

    @Autowired
    lateinit var greetingService: GreetingService

    @Autowired
    lateinit var greetingRepository: GreetingRepository

    // A command object
    class AnswerCaller(var caller: String? = null): Command()

    // The execution of a command
    // may through a (business) problem which will be dealt with in flow
    @Transactional
    fun answerCaller(command: AnswerCaller) {
        val caller = command.caller
        if (caller != null) greetingService.answer(caller)
    }

    // Another command
    class IdentifyCaller(var caller: String? = null): Command() {

        // callback to init object out of flow data
        override fun init() {
            caller = get(CallAnsweredAutomatically::class)?.caller
        }

    }

    // Again, the execution of a command
    @Transactional
    fun identifyCaller(command: IdentifyCaller): CallerStatus {
        return CallerStatus(greetingRepository.findByCaller(command.caller)?.isKnown())
    }

    // A "document" about the status of the app. May be returned by a
    // command and will be passed on to the flow.
    data class CallerStatus(val known: Boolean? = false): Value

}
