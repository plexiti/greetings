package com.plexiti.greetings.domain

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity
@Table(name="GREETING")
data class Greeting(
    @Column(name="NAME")
    val name: String = ""
) {
    @Id
    @SequenceGenerator(name="greeting_generator", sequenceName="greeting_sequence", allocationSize = 1)
    @GeneratedValue(generator = "greeting_generator")
    val id: Long? = null
}

@Repository
interface GreetingRepository : CrudRepository<Greeting, Long>
