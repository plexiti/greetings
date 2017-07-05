package com.plexiti.greetings.adapters.file

import com.plexiti.commons.application.Command
import com.plexiti.greetings.application.GreetingApplication.*
import com.plexiti.greetings.domain.Greeting
import org.apache.camel.Handler
import org.apache.camel.builder.RouteBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
class GreetingReader : RouteBuilder() {

    @Value("\${com.plexiti.greetings.path}")
    private lateinit var path: String;

    val options = "noop=true&idempotentKey=\${file:name}-\${file:modified}"

    override fun configure() {
        if (path.isNotEmpty()) {
            val file = File(path)
            if (file.exists()) {
                from("${file.toURI()}?${options}")
                .bean(object {
                    @Handler fun handle(caller: String) {
                        Command.issue(AnswerCaller(caller))
                    }
                })
            }
        }
    }

}
