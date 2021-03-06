package com.plexiti.greetings.adapters.rest

import com.plexiti.commons.application.Application
import com.plexiti.greetings.application.GreetingApplication.AnswerCaller
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
    lateinit var application: Application

    @RequestMapping("/greetings/{caller}")
    @ResponseBody
    fun getGreeting(@PathVariable caller: String): ResponseEntity<*> {
        if ("0xCOFFEEPOT".equals(caller, ignoreCase = true)) {
            return ResponseEntity<Any>(HttpStatus.I_AM_A_TEAPOT)
        }
        val document = application.execute(AnswerCaller(caller)) // process command synchronously
        return ResponseEntity(document, if (document is List<*>) HttpStatus.OK else HttpStatus.UNPROCESSABLE_ENTITY)
    }

}
