package com.plexiti.greetings

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@ComponentScan("com.plexiti")
@EnableJpaRepositories("com.plexiti")
class Greetings

fun main(args: Array<String>) {
    SpringApplication.run(Greetings::class.java, *args)
}
