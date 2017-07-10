package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.application.CommandStatus
import com.plexiti.commons.domain.EventId

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class EventsMapConverter : javax.persistence.AttributeConverter<Map<EventId, CommandStatus>, String> {

    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(events: Map<EventId, CommandStatus>?): String? {
        return if (events != null) objectMapper.writeValueAsString(events) else null
    }

    override fun convertToEntityAttribute(events: String?): Map<EventId, CommandStatus>? {
        return if (events != null) objectMapper.readValue(events, object: TypeReference<Map<EventId, CommandStatus>>() {}) else null
    }

}
