package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
interface Message {

    val context: Context
    val name: String

}

@MappedSuperclass
abstract class AbstractMessageEntity<ID: MessageId, S: MessageStatus>: Aggregate<ID>(), Message {

    @Embedded @AttributeOverride(name="name", column = Column(name="CONTEXT", nullable = false))
    override lateinit var context: Context
        protected set

    @Column(name="NAME", length = 128, nullable = false)
    override var name = this::class.simpleName!!
        protected set

    override val version: Int? = null
        @JsonIgnore get

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "FORWARDED_AT", nullable = true)
    var forwardedAt: Date? = null
        protected set

    @Lob
    @Column(name="JSON", columnDefinition = "text", nullable = false)
    lateinit var json: String
        protected set

    @Enumerated(EnumType.STRING) @JsonIgnore
    @Column(name="STATUS", length = 16, nullable = false)
    open lateinit var status: S
        @JsonIgnore set
        @JsonIgnore get

}

@MappedSuperclass
open class MessageId(value: String): AggregateId(value)

interface MessageStatus

enum class MessageType {
    Command, Document, Event
}
