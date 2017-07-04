package com.plexiti.commons

import org.junit.runner.RunWith
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@RunWith(SpringRunner::class)
@ContextConfiguration
@DataJpaTest
abstract class DataJpaIntegration {

    @Configuration
    @EntityScan("com.plexiti")
    @EnableJpaRepositories("com.plexiti")
    @ComponentScan("com.plexiti", "org.apache.camel")
    class DataJpaTestConfiguration

}
