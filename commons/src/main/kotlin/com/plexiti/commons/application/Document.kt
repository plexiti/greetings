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
    var valueReturned: Value? = null

    var problemOccured: Problem? = null

    constructor(command: Command): this() {
        this.name = command.name
        this.id = command.id
        this.command = command
        this.events = Event.store.findByRaisedByCommand_OrderByRaisedAtDesc(command.id)
        if (command.internals().valueReturned != null) {
            this.valueReturned = Value.store.findOne(command.internals().valueReturned)
        }
        this.problemOccured = command.internals().problemOccured
    }

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

}
