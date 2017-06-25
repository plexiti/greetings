package com.plexiti.commons.application

import com.plexiti.commons.adapters.db.EventStore
import com.plexiti.commons.domain.Event
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
class EventService {

    @Autowired
    lateinit var eventStore: EventStore

    @Transactional
    fun consume(json: String) {
        val eventId = eventStore.eventId(json)
        if (eventId != null) {
            val event = eventStore.findOne(eventId) ?: eventStore.save(Event.fromJson(json))
            event.internals.transitioned()
        }
    }

}
