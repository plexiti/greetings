package com.plexiti.commons.application

import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.domain.Event
import org.springframework.stereotype.Service

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
class CommandService {

    lateinit var eventStore: EventStore

    fun consume(json: String) {
        val event = Event.consume(json)
        if (event != null) transform(event)
    }

    fun transform(event: Event) {
        // trigger, complete and correlate
    }

}
