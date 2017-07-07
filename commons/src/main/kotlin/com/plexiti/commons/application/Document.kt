package com.plexiti.commons.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.plexiti.commons.domain.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class Document(): Message {

    override val type = MessageType.Document

    override lateinit var name: Name

    override lateinit var id: CommandId
        protected set

    lateinit var command: Command

    var events: List<Event>? = null

    @JsonDeserialize(`as` = DefaultValue::class)
    var value: Value? = null

    var problem: Problem? = null

    constructor(command: Command): this() {
        this.name = command.name
        this.id = command.id
        this.command = command
        this.events = Event.store.findByRaisedBy_OrderByRaisedAtDesc(command.id)
        if (command.internals().resultingIn != null) {
            this.value = Value.store.findOne(command.internals().resultingIn)
        }
        this.problem = command.internals().problem
    }

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

}
