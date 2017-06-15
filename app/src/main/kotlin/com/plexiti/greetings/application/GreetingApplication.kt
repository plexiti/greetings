package com.plexiti.greetings.application;

import com.plexiti.commons.application.Command
import com.plexiti.greetings.domain.Greeting
import com.plexiti.greetings.domain.GreetingRepository
import com.plexiti.greetings.domain.GreetingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
@Transactional
class GreetingApplication {

    @Value("\${com.plexiti.app.context}")
    private lateinit var context: String;

    @Autowired
    lateinit var greetingRepository: GreetingRepository

    @Autowired
    lateinit var greetingService: GreetingService

    class Answer(): Command() {

        lateinit var caller: String
        override val definition = 0
        override val target = context
        constructor(caller: String): this() {
            this.caller = caller
        }

    }

    fun answer(command: Answer): Greeting {
        return greetingService.answer(command.caller)
    }

    class Identify(): Command() {
        lateinit var greeting: String
        override val definition = 0
        override val target = context
        constructor(greeting: String): this() {
            this.greeting = greeting
        }
    }

    fun identify(command: Identify) {
        greetingRepository.findByGreeting(command.greeting)!!.contact()
    }

}
