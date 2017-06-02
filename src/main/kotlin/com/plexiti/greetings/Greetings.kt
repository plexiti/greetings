package com.plexiti.greetings

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Greetings

fun main(args: Array<String>) {
    SpringApplication.run(Greetings::class.java, *args)
}
