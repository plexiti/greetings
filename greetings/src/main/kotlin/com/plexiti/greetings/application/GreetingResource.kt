package com.plexiti.greetings.application

import com.plexiti.commons.domain.Value
import com.plexiti.greetings.domain.Greeting

class GreetingResource(): Value {

    lateinit var id: String
    lateinit var caller: String
    lateinit var greeting: String

    constructor(greeting: Greeting): this() {
        this.id = greeting.id.value
        this.caller = greeting.caller
        this.greeting = greeting.greeting
    }

}
