package com.plexiti.greetings

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class GreetingsApplication

fun main(args: Array<String>) {
    SpringApplication.run(GreetingsApplication::class.java, *args)
}
