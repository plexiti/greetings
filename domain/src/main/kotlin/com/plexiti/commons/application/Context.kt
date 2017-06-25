package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import javax.persistence.Column
import javax.persistence.Embeddable

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Embeddable
class Context() {

    @Column(name="NAME", length = 64, nullable = false)
    var name = "Default"
        @JsonValue get
        @JsonValue protected set

    constructor(name: String?): this() {
        this.name = name ?: "Default"
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Context) return false
        if (name != other.name) return false
        return true
    }

    companion object {
        var home = Context()
    }

}
