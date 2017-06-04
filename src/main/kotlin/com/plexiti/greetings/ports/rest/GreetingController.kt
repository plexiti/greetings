package com.plexiti.greetings.ports.rest

import com.plexiti.greetings.application.Route
import com.plexiti.greetings.application.GreetingApplication.*
import com.plexiti.greetings.domain.Greeting
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.apache.camel.ProducerTemplate

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@RestController
class GreetingController {

    @Autowired
    lateinit var router: ProducerTemplate

    data class GreetingResource(
        val id: String?,
        val name: String
    )

    @RequestMapping("/greetings/{caller}")
    @ResponseBody
    fun getGreeting(@PathVariable caller: String): ResponseEntity<*> {
        if ("0xCAFEBABE".equals(caller, ignoreCase = true)) {
            return ResponseEntity<Any>(HttpStatus.I_AM_A_TEAPOT)
        }
        val greeting = router.requestBody(Route.Sync.GreetingApplication, GreetCommand(caller), Greeting::class.java)
        return ResponseEntity(GreetingResource(greeting.id?.value, greeting.name), HttpStatus.OK)
    }

}
