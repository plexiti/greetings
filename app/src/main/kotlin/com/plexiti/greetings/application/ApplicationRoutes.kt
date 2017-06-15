package com.plexiti.greetings.application

import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
object Route {
    object Sync {
        val answer = "direct:answer"
        val identify = "direct:identify"
    }
}

@Component
class ApplicationRouteBuilder : RouteBuilder() {

    override fun configure() {
        from(Route.Sync.answer).bean(GreetingApplication::class.java, "answer")
        from(Route.Sync.identify).bean(GreetingApplication::class.java, "identify")
    }

}
