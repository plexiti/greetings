package com.plexiti.greetings.domain

import com.plexiti.commons.domain.Aggregate
import com.plexiti.commons.domain.EntityId
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity @Table(name="GREETING")
class Greeting (

    @Column(name="NAME")
    val name: String = ""

): Aggregate<GreetingId>(GreetingId())

@Repository
interface GreetingRepository : CrudRepository<Greeting, GreetingId>

class GreetingId(value: String? = null): EntityId(value)
