package com.plexiti.commons.domain

import java.io.Serializable
import java.util.*
import javax.persistence.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@MappedSuperclass
abstract class Aggregate<ID>(id: ID? = null): AbstractEntity<ID>(id)

@MappedSuperclass
abstract class AbstractEntity<ID>(@EmbeddedId val id: ID? = null) {

    @Version val version: Int? = null

    fun isNew(): Boolean {
        return version == null
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
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
abstract class EntityId(value: String? = null): Serializable {

    @Column(name = "ID", length = 36)
    var value = value ?: UUID.randomUUID().toString()

}
