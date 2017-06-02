package com.plexiti.greetings

import org.apache.camel.builder.RouteBuilder
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component
@Profile("prod")
class GreetingsRoute : RouteBuilder() {

    val camelFileUri = "file:///Users/martin/Temp/camel"
    val camelFileConsumerOptions = "noop=true&idempotentKey=\${file:name}-\${file:modified}"

    override fun configure() {
        from("${camelFileUri}?${camelFileConsumerOptions}")
            .bean(GreetingsReader::class.java)
    }

}
