package com.plexiti.greetings.ports.rest

import com.plexiti.commons.application.CommandId
import com.plexiti.greetings.application.GreetingApplication.GreetCommand
import com.plexiti.greetings.application.Route
import com.plexiti.greetings.domain.Greeting
import org.apache.camel.ProducerTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@RestController
class GreetingController {

    @Autowired
    lateinit var router: ProducerTemplate

    data class GreetingResource(val id: String, val name: String) {
        constructor(greeting: Greeting): this(greeting.id?.value!!, greeting.name)
    }

    @RequestMapping("/greetings/{caller}")
    @ResponseBody
    fun getGreeting(@PathVariable caller: String): ResponseEntity<*> {
        if ("0xCAFEBABE".equals(caller, ignoreCase = true)) {
            return ResponseEntity<Any>(HttpStatus.I_AM_A_TEAPOT)
        }
        val command = GreetCommand(id = CommandId(), caller = caller)
        val greeting = router.requestBody(Route.Sync.GreetingApplication, command) as Greeting
        return ResponseEntity(GreetingResource(greeting), HttpStatus.OK)
    }

}
