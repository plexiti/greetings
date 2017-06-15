package com.plexiti.commons.domain

import org.apache.camel.component.jpa.Consumed
import java.io.Serializable
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@MappedSuperclass
abstract class AbstractMessageEntity<TYPE: Message, ID: MessageId>: AbstractEntity<ID>(), Message {

    @Enumerated(EnumType.STRING)
    @Column(name="TYPE", columnDefinition = "varchar(16)")
    override lateinit var type: MessageType
        protected set

    @Column(name="NAME", columnDefinition = "varchar(128)")
    override lateinit var name: String
        protected set

    @Column(name="ORIGIN", columnDefinition = "varchar(64)")
    override var origin: String? = null
        protected set

    @Column(name="DEFINITION")
    override var definition: Int = 0

    @Lob
    @Column(name="JSON", columnDefinition = "text")
    lateinit var json: String
        protected set

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PUBLISHED_AT")
    var publishedAt: Date? = null
        private set

    @Consumed
    fun setPublished() {
        if (publishedAt == null)
            publishedAt = Date()
    }

    fun isPublished(): Boolean {
        return publishedAt != null
    }

}

@MappedSuperclass
open class MessageId(value: String): AggregateId(value)

interface Message {

    val type: MessageType
    val id: Serializable
    val origin: String?
    val name: String
    val definition: Int

}

enum class MessageType {
    Event, Command, Document
}
