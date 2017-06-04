package com.plexiti.greetings

import org.camunda.bpm.application.ProcessApplication
import org.camunda.bpm.spring.boot.starter.SpringBootProcessApplication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@ComponentScan("com.plexiti")
@EnableJpaRepositories("com.plexiti")
@ProcessApplication("greetings")
class Greetings: SpringBootProcessApplication()

fun main(args: Array<String>) {
    SpringApplication.run(Greetings::class.java, *args)
}
