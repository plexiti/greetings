package com.plexiti.greetings.domain

import com.plexiti.commons.domain.*
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity
@Table(name="GREETINGS")
class Greeting (

    @Column(name="NAME")
    val name: String = ""

): Aggregate<GreetingId>(GreetingId()) {

    class GreetingCreated(greeting: Greeting): Event(greeting) {
        val name = greeting.name
    }

    init {
        raise(GreetingCreated(this))
    }

}

@Repository
interface GreetingRepository : CrudRepository<Greeting, GreetingId>

class GreetingId(value: String? = null): AggregateId(value)
