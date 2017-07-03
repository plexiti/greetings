package com.plexiti.commons.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.adapters.db.DocumentEntityRepository
import com.plexiti.commons.adapters.db.InMemoryDocumentEntityRepository
import com.plexiti.commons.adapters.db.KClassAttributeConverter
import com.plexiti.commons.domain.*
import com.plexiti.utils.hash
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.*
import javax.persistence.*
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Entity
@Table(name="DOCUMENTS")
open class DocumentEntity(): Aggregate<DocumentId>() {

    @Column(name = "CREATED_AT", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    var createdAt = Date()
        internal set

    @Column(name="TYPE", columnDefinition = "text", nullable = false, length = 256)
    @Convert(converter = KClassAttributeConverter::class)
    lateinit var type: KClass<*>
        protected set

    @Lob
    @Column(name="JSON", columnDefinition = "text", nullable = false)
    lateinit var json: String
        protected set

    constructor(id: DocumentId, type: KClass<*>, json: String): this() {
        this.id = id
        this.type = type
        this.json = json
    }

    companion object {

        internal var store: DocumentEntityRepository = InMemoryDocumentEntityRepository()

        fun create(document: Any): DocumentEntity {
            val text = ObjectMapper().writeValueAsString(document)
            val id = DocumentId(hash(text))
            return store.findOne(id) ?: DocumentEntity(id, document::class, text)
        }

        fun <D: Any> fromJson(json: String, type: KClass<D>): D {
            return ObjectMapper().readValue(json, type.java)
        }

    }

}

open class DocumentId(value: String = ""): MessageId(value)

@NoRepositoryBean
interface DocumentRepository<D>: CrudRepository<D, DocumentId>
