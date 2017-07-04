package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.domain.*
import com.plexiti.commons.domain.MessageType.Discriminator.event
import java.util.*
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.Transient

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class Flow: Command() {

    override val type = MessageType.Flow

}

@Entity
@DiscriminatorValue(MessageType.Discriminator.flow)
@NamedQueries(
    NamedQuery(
        name = "FlowForwarder",
        query = "select f from FlowEntity f where f.forwardedAt is null"
    )
)
class FlowEntity(): CommandEntity() {

    @Transient
    override val type = MessageType.Flow

}

open class TokenId(value: String = ""): AggregateId(value)

class FlowEvent(): Event() {

    constructor(name: Name): this() {
        this.id = EventId(UUID.randomUUID().toString())
        this.name = name
        this.raisedAt = Date()
    }

}

@JsonInclude(NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
class FlowMessage() {

    lateinit var type: MessageType

    var command: Command? = null
    var event: Event? = null
    var result: Result? = null

    lateinit var flowId: CommandId
    var tokenId: TokenId? = null

    var events: List<Event> = emptyList()

    constructor(event: Event, flowId: CommandId): this() {
        this.type = event.type
        this.event = event
        this.flowId = flowId
    }

    constructor(command: Command, flowId: CommandId, tokenId: TokenId? = null): this() {
        this.type = command.type
        this.command = command
        this.flowId = flowId
        this.tokenId = tokenId
    }

    constructor(result: Result, flowId: CommandId, tokenId: TokenId? = null): this() {
        this.type = result.type
        this.result = result
        this.flowId = flowId
        this.tokenId = tokenId
    }

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

    companion object {

        fun fromJson(json: String): FlowMessage {
            return ObjectMapper().readValue(json, FlowMessage::class.java)
        }

    }

}
