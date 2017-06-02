package com.plexiti.greetings.ports.file

import org.apache.camel.builder.RouteBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.File

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Profile("prod", "camel")
class GreetingRoute : RouteBuilder() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${com.plexiti.greetings.path}")
    lateinit var greetingsPath: String

    val camelFileConsumerOptions = "noop=true&idempotentKey=\${file:name}-\${file:modified}"

    override fun configure() {
        if (greetingsPath.isNotEmpty()) {
            val file = File(greetingsPath)
            logger.info("Configuring route for $greetingsPath ...")
            if (file.exists()) {
                from("${file.toURI()}?${camelFileConsumerOptions}")
                    .bean(GreetingReader::class.java)
                logger.info("Configuring route for $greetingsPath successful.")
            }
        }
    }

}
