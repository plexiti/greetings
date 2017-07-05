package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import javax.persistence.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@MappedSuperclass
abstract class Aggregate<ID: AggregateId>: AbstractEntity<ID>() {

    @Version
    open val version: Int? = null;

    fun isNew(): Boolean {
        return version == null
    }

}

@MappedSuperclass
abstract class AbstractEntity<ID: Serializable> {

    @EmbeddedId open lateinit var id: ID

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractEntity<*>) return false
        if (id != other.id) return false
        return true
    }

}

@Embeddable
@MappedSuperclass
abstract class AggregateId(value: String): Serializable {

    @Column(name = "ID", length = 36, nullable = false)
    open var value: String = value
        @JsonValue get
        @JsonValue protected set

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AggregateId) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

}
