package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.adapters.db.ValueStore
import com.plexiti.utils.hash
import com.plexiti.utils.scanPackageForClassNames
import com.plexiti.utils.scanPackageForNamedClasses
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.*
import javax.persistence.*
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@JsonInclude(NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
interface Value: Named {

    override val name: Name
        get() = Name(name = this::class.java.simpleName)

    companion object {

        var store = ValueStore()

        fun <D: Value> fromJson(json: String, type: KClass<D>): D {
            return ObjectMapper().readValue(json, type.java)
        }

    }

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

}

@Entity
@Table(name="VALUES")
open class StoredValue(): Aggregate<ValueId>() {

    @Column(name = "CREATED_AT", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    var createdAt = Date()
        internal set

    @Embedded
    var name = Name(name = this::class.simpleName!!)
        protected set

    @Lob
    @Column(name="JSON", columnDefinition = "text", nullable = false)
    lateinit var json: String
        protected set

    constructor(id: ValueId, name: Name, json: String): this() {
        this.id = id
        this.name = name
        this.json = json
    }

}

class DefaultValue : Value

open class ValueId(value: String = ""): MessageId(value) {

    constructor(value: Value): this(hash(value.toJson()))

}

@NoRepositoryBean
interface ValueStore<D>: CrudRepository<D, ValueId>

interface Named {
    val name: Name
}
