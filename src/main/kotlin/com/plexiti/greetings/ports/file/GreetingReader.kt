package com.plexiti.greetings.ports.file

import com.plexiti.greetings.application.GreetingApplication.*
import com.plexiti.greetings.application.Route
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
    lateinit var greetingsPath: String

    val consumerOptions = "noop=true&idempotentKey=\${file:name}-\${file:modified}"

    override fun configure() {
        if (greetingsPath.isNotEmpty()) {
            val file = File(greetingsPath)
            if (file.exists()) {
                from("${file.toURI()}?${consumerOptions}")
                    .convertBodyTo(String::class.java)
                    .convertBodyTo(GreetCommand::class.java)
                    .to(Route.Sync.GreetingApplication)
            }
        }
    }

}
