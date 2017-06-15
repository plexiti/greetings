package com.plexiti.greetings.application;

import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandExecutor
import com.plexiti.greetings.domain.Greeting
import com.plexiti.greetings.domain.GreetingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service @Transactional
class GreetingApplication: CommandExecutor() {

    @Autowired
    lateinit var greetingService: GreetingService

    class Answer(): Command() {

        lateinit var caller: String

        constructor(caller: String): this() {
            this.caller = caller
        }

    }

    fun answer(command: Answer): Greeting {
        return greetingService.answer(command.caller)
    }

    class Identify(): Command() {

        lateinit var greeting: String

        constructor(greeting: String): this() {
            this.greeting = greeting
        }

    }

    fun identify(command: Identify) {
        greetingService.greetingRepository.findByGreeting(command.greeting)!!.contact()
    }

}
