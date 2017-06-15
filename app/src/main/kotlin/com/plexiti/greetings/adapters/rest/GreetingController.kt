package com.plexiti.greetings.adapters.rest

import com.plexiti.commons.application.Command
import com.plexiti.greetings.application.GreetingApplication.Answer
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
        constructor(greeting: Greeting): this(greeting.id?.value!!, greeting.greeting)
    }

    @RequestMapping("/greetings/{caller}")
    @ResponseBody
    fun getGreeting(@PathVariable caller: String): ResponseEntity<*> {
        if ("0xCAFEBABE".equals(caller, ignoreCase = true)) {
            return ResponseEntity<Any>(HttpStatus.I_AM_A_TEAPOT)
        }
        val greeting = Command.issue(Answer(caller))
        return ResponseEntity(GreetingResource(greeting), HttpStatus.OK)
    }

}
