package com.plexiti.greetings.ports.file

import com.plexiti.greetings.application.GreetingService
import com.plexiti.greetings.application.GreetingService.*
import org.apache.camel.Handler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class GreetingReader {

    @Autowired
    lateinit var greetingService: GreetingService

    @Handler
    fun read(caller: String) {
        val greeting = greetingService.execute(GreetCommand(caller))
    }

}
