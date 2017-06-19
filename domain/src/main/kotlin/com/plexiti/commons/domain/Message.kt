package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@MappedSuperclass
abstract class AbstractMessageEntity<ID: MessageId, S: MessageStatus>: Aggregate<ID>() {

    @Transient
    val type = this::class.simpleName

    @Column(name="CONTEXT", length = 64)
    lateinit var context: String
        protected set

    @Column(name="NAME", length = 128)
    lateinit var name: String
        protected set

    @Column(name="DEFINITION")
    var definition: Int = 0
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
    var forwardedAt: Date? = null
        protected set

    @Lob
    @Column(name="JSON", columnDefinition = "text")
    lateinit var json: String
        protected set

}

@MappedSuperclass
open class MessageId(value: String): AggregateId(value)

interface MessageStatus
