package com.plexiti.commons.domain

import javax.persistence.Column
import javax.persistence.Embeddable

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Embeddable
class Context() {

    @Column(name="NAME", length = 64, nullable = false)
    lateinit var name: String
        protected set

    constructor(name: String): this() {
        this.name = name
    }

}
