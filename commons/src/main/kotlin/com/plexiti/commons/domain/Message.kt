package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.*
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
interface Message {

    val id: MessageId
    val name: Name
    val type: MessageType

}

@MappedSuperclass
abstract class AbstractMessageEntity<ID: MessageId, S: MessageStatus>: Aggregate<ID>(), Message {

    @Embedded
    override var name = Name(name = this::class.simpleName!!)
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

    Event, Command, Flow, Result, Document;

    companion object Discriminator {

        const val event = "Event"
        const val command = "Command"
        const val flow = "Flow"

    }

}

@Embeddable
class Name() {

    @Column(name="CONTEXT", length = 64, nullable = false)
    var context = "Default"
        @JsonIgnore get
        @JsonIgnore internal set (context) {
            field = context; qualified
        }

    @Column(name="NAME", length = 128, nullable = false)
    var name = "Default"
        @JsonIgnore get
        @JsonIgnore internal set (simpleName) {
            field = simpleName; qualified
        }

    @Transient
    var qualified: String = "Default_Default"
        @JsonValue get() {
            field = context + "_" + name
            return field
        }
        @JsonValue protected set(fullName) {
            if (fullName.indexOf("_") < 1 || fullName.length < 3)
                throw IllegalArgumentException("Full name must consist of two parts (name and name) separated by an underscore ('_').")
            field = fullName
            context = fullName.substring(0, fullName.indexOf("_"))
            name = fullName.substring(fullName.indexOf("_") + 1, fullName.length)
        }

    constructor(qualified: String): this() {
        this.qualified = qualified
    }

    constructor(context: String = default.context, name: String): this() {
        this.context = context
        this.name = name
    }

    override fun hashCode(): Int {
        return qualified.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Name) return false
        if (qualified != other.qualified) return false
        return true
    }

    companion object {
        var default = Name()
    }

}