package com.plexiti.greetings.application;

import com.plexiti.commons.application.Command
import com.plexiti.commons.domain.Value
import com.plexiti.greetings.domain.Greeting
import com.plexiti.greetings.domain.Greeting.CallAnsweredAutomatically
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

    class AnswerCaller(): Command() {

        lateinit var caller: String

        constructor(caller: String): this() {
            this.caller = caller
        }

    }

    fun answerCaller(command: AnswerCaller) {
        greetingService.answer(command.caller)
    }

    class IdentifyCaller(): Command() {

        lateinit var greeting: String

        override fun construct() {
            greeting = event(CallAnsweredAutomatically::class)!!.greeting
        }

        constructor(greeting: String): this() {
            this.greeting = greeting
        }

    }

    data class CallerIdentified(val known: Boolean? = null): Value

    fun identifyCaller(command: IdentifyCaller): CallerIdentified {
        return CallerIdentified(greetingService.greetingRepository.findByGreeting(command.greeting)!!.isKnown())
    }

}
