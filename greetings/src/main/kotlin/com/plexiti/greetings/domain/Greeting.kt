package com.plexiti.greetings.domain

import com.plexiti.commons.domain.Aggregate
import com.plexiti.commons.domain.AggregateId
import com.plexiti.commons.domain.Value
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.Event.Companion.raise
import com.plexiti.greetings.domain.Greeting.CallAnsweredAutomatically
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
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

    @Column(name="CALLER")
    lateinit var caller: String
        private set

    @Column(name="GREETING")
    lateinit var greeting: String
        private set

    @Column(name="CONTACTS")
    var contacts = 0
        private set

    class GreetingCreated(greeting: Greeting? = null): Event(greeting) {
        val caller = greeting?.caller
        val greeting = greeting?.greeting
    }

    class CallAnsweredAutomatically(greeting: Greeting? = null): Event(greeting) {

        val caller = greeting?.caller
        val greeting = greeting?.greeting

    }

    class CallerContactedPersonally: Event() {

        lateinit var caller: String

        override fun construct() {
            val event = event(Greeting.CallAnsweredAutomatically::class)!!
            caller = event.caller!!
        }

    }

    companion object {

        fun create(caller: String, greeting: String = String.format("Hello World, %s", caller)): Greeting {
            val new = Greeting()
            new.id = GreetingId(UUID.randomUUID().toString())
            new.greeting = greeting
            new.caller = caller
            raise(GreetingCreated(new))
            return new
        }

    }

    fun contact() {
        contacts++
    }

    fun isKnown(): Boolean {
        return contacts > 1
    }

}

@Repository
interface GreetingRepository : CrudRepository<Greeting, GreetingId> {

    fun findByGreeting(greeting: String): Greeting?

}

class GreetingId(value: String = ""): AggregateId(value)

@Component
class GreetingService {

    @Autowired
    lateinit var greetingRepository: GreetingRepository

    fun answer(caller: String): Greeting {
        val answer = String.format("Hello World, %s", caller)
        val greeting = greetingRepository.findByGreeting(answer) ?: Greeting.create(caller, answer)
        greeting.contact()
        greetingRepository.save(greeting)
        raise(CallAnsweredAutomatically(greeting))
        return greeting
    }

}
