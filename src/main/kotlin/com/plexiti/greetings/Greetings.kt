package com.plexiti.greetings

import org.camunda.bpm.application.ProcessApplication
import org.camunda.bpm.spring.boot.starter.SpringBootProcessApplication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@ComponentScan(scan)
@EntityScan(scan)
@EnableJpaRepositories(scan)
@ProcessApplication(app)
class Greetings: SpringBootProcessApplication()

const val app = "greetings";
const val scan = "com.plexiti";

fun main(args: Array<String>) {
    SpringApplication.run(Greetings::class.java, *args)
}
