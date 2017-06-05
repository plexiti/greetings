package com.plexiti.greetings.domain

import com.plexiti.commons.domain.Aggregate
import com.plexiti.commons.domain.AggregateId
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Events
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.Table


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity @Table(name="GREETINGS")
class Greeting (

    @Column(name="NAME")
    val name: String = ""

): Aggregate<GreetingId>(GreetingId()) {

    @Entity @DiscriminatorValue("CallerGreetedEvent")
    class CallerGreetedEvent(greeting: Greeting? = null) : Event(greeting)
    init {
        Events.raise(CallerGreetedEvent(this))
    }

}

@Repository
interface GreetingRepository : CrudRepository<Greeting, GreetingId>

class GreetingId(value: String? = null): AggregateId(value)
