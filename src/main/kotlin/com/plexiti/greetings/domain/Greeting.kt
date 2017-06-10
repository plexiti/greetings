package com.plexiti.greetings.domain

import com.plexiti.commons.domain.*
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity
@Table(name="GREETINGS")
class Greeting: Aggregate<GreetingId>() {

    @Column(name="NAME")
    lateinit var name: String private set

    class GreetingCreated(greeting: Greeting): Event(greeting) {
        val name = greeting.name
    }

    companion object {

        fun create(name: String): Greeting {
            val new = Greeting()
            new.id = GreetingId(UUID.randomUUID().toString())
            new.name = name
            Event.raise(GreetingCreated(new))
            return new
        }

    }

}


@Repository
interface GreetingRepository : CrudRepository<Greeting, GreetingId>

class GreetingId(value: String = ""): AggregateId(value)
