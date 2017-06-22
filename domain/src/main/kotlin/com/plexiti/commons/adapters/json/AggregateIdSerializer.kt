package com.plexiti.commons.adapters.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.plexiti.commons.domain.AggregateId


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class AggregateIdSerializer: JsonSerializer<AggregateId>() {

    override fun serialize(value: AggregateId, jgen: JsonGenerator, provider: SerializerProvider) {
        jgen.writeString(value.value)
    }

}

class AggregateIdDeserializer: JsonDeserializer<AggregateId>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): AggregateId {
        return p.readValueAs(ctxt.contextualType.rawClass) as AggregateId
    }

}
