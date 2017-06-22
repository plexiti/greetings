package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
interface MessageInterface {

    val type: MessageType
    val context: Context
    val name: String
    val definition: Int
    val forwardedAt: Date?

}

@MappedSuperclass
abstract class AbstractMessageEntity<ID: MessageId, S: MessageStatus>: Aggregate<ID>(), MessageInterface {

    override abstract val type: MessageType

    @Embedded @AttributeOverride(name="name", column = Column(name="CONTEXT"))
    override lateinit var context: Context
        protected set

    @Column(name="NAME", length = 128)
    override var name = this::class.simpleName!!
        protected set

    @Column(name="DEFINITION")
    override var definition: Int = 0
        protected set

    override val version: Int? = null
        @JsonIgnore get

    @Enumerated(EnumType.STRING) @JsonIgnore
    @Column(name="STATUS", length = 16)
    open lateinit var status: S
        @JsonIgnore set
        @JsonIgnore get

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "FORWARDED_AT")
    override var forwardedAt: Date? = null
        protected set

    @Lob
    @Column(name="JSON", columnDefinition = "text")
    lateinit var json: String
        protected set

}

@MappedSuperclass
open class MessageId(value: String): AggregateId(value)

interface MessageStatus

enum class MessageType {
    Command, Document, Event
}
