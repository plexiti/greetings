package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.adapters.db.DocumentRepository
import com.plexiti.commons.domain.*
import com.plexiti.utils.hash
import com.plexiti.utils.scanPackageForAssignableClasses
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.*
import javax.persistence.*
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
interface Document: Message {

    override val id: DocumentId
        @JsonIgnore get() = DocumentId(toJson())

    override val type: MessageType
        @JsonIgnore get() = MessageType.Document

    override val name: Name
        get() = Name(name = this::class.java.simpleName)

    companion object {

        internal var types = scanPackageForAssignableClasses("com.plexiti", Document::class.java)
            .map { it.newInstance() as Document }
            .associate { Pair(it.name.qualified, it::class) }

        internal var repository = DocumentRepository()

        fun <D: Document> fromJson(json: String, type: KClass<D>): D {
            return ObjectMapper().readValue(json, type.java)
        }

    }

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

}

class DocumentImpl: Document

@Entity
@Table(name="DOCUMENTS")
open class DocumentEntity(): Aggregate<DocumentId>() {

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

    constructor(id: DocumentId, name: Name, json: String): this() {
        this.id = id
        this.name = name
        this.json = json
    }

}

open class DocumentId(value: String = ""): MessageId(value) {

    constructor(document: Document): this() {
        value = hash(document.toJson())
    }

}

@NoRepositoryBean
interface DocumentRepository<D>: CrudRepository<D, DocumentId>
