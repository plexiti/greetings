package com.plexiti.greetings.application;

import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandEntity
import com.plexiti.greetings.domain.Greeting
import com.plexiti.greetings.domain.GreetingRepository
import org.apache.camel.Handler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
@Transactional
class GreetingApplication {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var greetingRepository: GreetingRepository

    class GreetCommand (): Command() {
        lateinit var caller: String
        override val definition = 0
        constructor(caller: String): this() {
            this.caller = caller
        }
    }

    @Handler
    fun greetCaller(greet: GreetCommand): Greeting {
        val greeting = Greeting.create(String.format("Hello World, %s", greet.caller))
        greetingRepository.save(greeting)
        logger.info("Greeting #${greeting.id}: ${greeting.greeting}")
        return greeting
    }

}
