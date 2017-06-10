package com.plexiti.commons.domain

import org.apache.camel.component.jpa.Consumed
import java.io.Serializable
import java.util.*
import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@MappedSuperclass
abstract class AbstractMessageEntity<ID: MessageId>: AbstractEntity<ID>(), Message {

    @Column(name="TYPE", columnDefinition = "varchar(128)")
    override lateinit var type: String
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

    val id: Serializable
    val type: String
    val definition: Int

}
