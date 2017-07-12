package com.plexiti.greetings.domain

import com.plexiti.commons.domain.*
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

    // A domain event (raised by the local domain when an aggregate is created
    class GreetingCreated(greeting: Greeting? = null): Event(greeting) {

        val caller = greeting?.caller
        val greeting = greeting?.greeting

    }

    // Another domain event (also raised be the local domain).
    // It also triggers a flow "DealWithCaller")
    class CallAnsweredAutomatically: Event {

        lateinit var caller: String
        lateinit var greeting: String

        constructor(): super()

        constructor(greeting: Greeting): super(greeting) {
            this.caller = greeting.caller
            this.greeting = greeting.greeting
        }

    }

    // Again, a domain event (but this one happends to be raised be the flow)
    class CallerContactedPersonally: Event()

    // When a caller calls several times, we know that
    fun contact() {
        contacts++
    }

    class FraudDetected : Problem()

    // We consider people calling more often than once well known pals
    fun isKnown(): Boolean {
        // But we don't answer to Bernie Madoff
        if (caller == "Madoff")
            throw FraudDetected()
        return contacts > 1
    }

    companion object {

        // A Greetings Factory :-)
        fun create(caller: String, greeting: String = String.format("Hello World, %s!", caller)): Greeting {
            val new = Greeting()
            new.id = GreetingId(UUID.randomUUID().toString())
            new.greeting = greeting
            new.caller = caller
            raise(GreetingCreated(new))
            return new
        }

    }

}

@Repository
interface GreetingRepository : CrudRepository<Greeting, GreetingId> {

    fun findByCaller(caller: String?): Greeting?

}

class GreetingId(value: String = ""): AggregateId(value)

@Component
class GreetingService {

    @Autowired
    lateinit var greetingRepository: GreetingRepository

    // A small service to answer a caller automatically, it returns a greeting
    fun answer(caller: String): Greeting {
        val greeting = greetingRepository.findByCaller(caller)
            ?: Greeting.create(caller)
        greeting.contact()
        greetingRepository.save(greeting)
        raise(CallAnsweredAutomatically(greeting))
        return greeting
    }

}
