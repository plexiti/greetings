package com.plexiti.greetings

import org.springframework.data.repository.CrudRepository


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
interface GreetingRepository : CrudRepository<Greeting, Long>
