package com.plexiti.greetings.ports.rest

import com.plexiti.greetings.application.GreetingService
import com.plexiti.greetings.application.GreetingService.*
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
    lateinit var greetingService: GreetingService

    data class GreetingResource(
        val id: Long?,
        val name: String
    )

    @RequestMapping("/greetings/{caller}")
    @ResponseBody
    fun getGreeting(@PathVariable caller: String): ResponseEntity<*> {
        if ("0xCAFEBABE".equals(caller, ignoreCase = true)) {
            return ResponseEntity<Any>(HttpStatus.I_AM_A_TEAPOT)
        }
        val greeting = greetingService.execute(GreetCommand(caller))
        return ResponseEntity(GreetingResource(greeting.id, greeting.name), HttpStatus.OK)
    }

}
