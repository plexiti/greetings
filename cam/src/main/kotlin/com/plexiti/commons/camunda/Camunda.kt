package com.plexiti.commons.camunda

import org.camunda.bpm.application.ProcessApplication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.support.SpringBootServletInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.boot.builder.SpringApplicationBuilder



@SpringBootApplication
@ComponentScan(group)
@ProcessApplication(name)
class Camunda: SpringBootServletInitializer() {

    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
        return application.sources(Camunda::class.java)
    }

}

const val name = "camunda";
const val group = "com.plexiti";

fun main(args: Array<String>) {
    SpringApplication.run(Camunda::class.java, *args)
}
