package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.domain.EventId

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class EventIdListConverter: javax.persistence.AttributeConverter<List<EventId>, String> {

    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(eventIds: List<EventId>?): String? {
        return if (eventIds != null) objectMapper.writeValueAsString(eventIds) else null
    }

    override fun convertToEntityAttribute(data: String?): List<EventId>? {
        return if (data != null) objectMapper.readValue(data, object: TypeReference<List<EventId>>() {}) else null
    }

}
