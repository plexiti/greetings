package com.plexiti.greetings

import org.camunda.bpm.application.ProcessApplication
import org.camunda.bpm.spring.boot.starter.SpringBootProcessApplication
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@ComponentScan(group)
@EntityScan(group)
@EnableJpaRepositories(group)
@EnableRabbit
@ProcessApplication(name)
class Greetings: SpringBootProcessApplication()

const val name = "greetings";
const val group = "com.plexiti";

fun main(args: Array<String>) {
    SpringApplication.run(Greetings::class.java, *args)
}
