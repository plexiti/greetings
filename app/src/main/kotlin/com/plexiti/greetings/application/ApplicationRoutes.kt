package com.plexiti.greetings.application

import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
object Route {
    object Sync {
        val GreetingApplication = "direct:greeting"
    }
}

@Component
class ApplicationRouteBuilder : RouteBuilder() {

    override fun configure() {
        from(Route.Sync.GreetingApplication).bean(GreetingApplication::class.java)
    }

}
