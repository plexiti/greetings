package com.plexiti.commons.domain

import org.junit.runner.RunWith
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
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
class DataJpaTest {

    @Configuration
    @EntityScan("com.plexiti.commons")
    @EnableJpaRepositories("com.plexiti.commons")
    class DataJpaTestConfiguration

}
