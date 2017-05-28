package com.plexiti.greetings

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@RestController
class GreetingsController {

    @RequestMapping("/greetings/{caller}")
    fun getGreeting(@PathVariable caller: String): ResponseEntity<*> {

        if ("0xCAFEBABE".equals(caller, ignoreCase = true)) {
            return ResponseEntity<Any>(HttpStatus.I_AM_A_TEAPOT)
        }

        val greeting = String.format("Hello World, %s", caller)
        return ResponseEntity(greeting, HttpStatus.OK)

    }

}