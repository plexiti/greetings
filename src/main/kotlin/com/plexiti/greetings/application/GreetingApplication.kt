package com.plexiti.greetings.application;

import com.plexiti.greetings.domain.Greeting
import com.plexiti.greetings.domain.GreetingRepository
import org.apache.camel.Handler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
@Transactional
class GreetingApplication {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var greetingRepository: GreetingRepository

    data class GreetCommand(
        val caller: String
    )

    @Handler
    fun greetCaller(command: GreetCommand): Greeting {
        val greeting = Greeting(name = String.format("Hello World, %s", command.caller))
        greetingRepository.save(greeting)
        logger.info("Greeting #${greeting.id}: ${greeting.name}")
        return greeting
    }

}
