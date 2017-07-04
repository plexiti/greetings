package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.application.*
import com.plexiti.commons.application.DocumentRepository
import com.plexiti.utils.hash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component @NoRepositoryBean
class DocumentRepository: DocumentRepository<Document>, ApplicationContextAware {

    @Autowired
    private var delegate: DocumentEntityRepository = InMemoryDocumentEntityRepository()

    internal fun type(qName: String): KClass<out Document> {
        return Document.types.get(qName) ?: throw IllegalArgumentException("Document type '$qName' is not mapped to a local object type!")
    }

    private fun toDocument(entity: DocumentEntity?): Document? {
        return  if (entity != null) Document.fromJson(entity.json, type(entity.name.qualified)) else null
    }

    private fun toEntity(document: Document?): DocumentEntity? {
        if (document != null) {
            val text = ObjectMapper().writeValueAsString(document)
            val id = DocumentId(document)
            return delegate.findOne(id) ?: DocumentEntity(id, document.name(), text)
        }
        return null
    }

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Document.repository = this
    }

    override fun exists(id: DocumentId?): Boolean {
        return delegate.exists(id)
    }

    override fun findOne(id: DocumentId?): Document? {
        return toDocument(delegate.findOne(id))
    }

    fun documentId(json: String?): DocumentId? {
        return if (json != null) DocumentId(hash(json)) else null
    }

    fun findOne(json: String): Document? {
        return findOne(documentId(json))
    }

    override fun findAll(): MutableIterable<Document> {
        return delegate.findAll().mapTo(ArrayList(), { toDocument(it)!! })
    }

    override fun findAll(ids: MutableIterable<DocumentId>?): MutableIterable<Document> {
        return delegate.findAll(ids).mapTo(ArrayList(), { toDocument(it)!! })
    }

    override fun <S : Document?> save(document: S): S {
        @Suppress("unchecked_cast")
        return toDocument(delegate.save(toEntity(document))) as S
    }

    override fun <S : Document?> save(documents: MutableIterable<S>?): MutableIterable<S> {
        return documents?.mapTo(ArrayList(), { save(it) })!!
    }

    override fun count(): Long {
        return delegate.count()
    }

    override fun delete(entities: MutableIterable<Document>?) {
        delegate.delete(entities?.map { toEntity(it) })
    }

    override fun delete(document: Document?) {
        delegate.delete(toEntity(document))
    }

    override fun delete(id: DocumentId?) {
        delegate.delete(id)
    }

    override fun deleteAll() {
        delegate.deleteAll()
    }

}

@Repository
internal interface DocumentEntityRepository: DocumentRepository<DocumentEntity>

@NoRepositoryBean
class InMemoryDocumentEntityRepository: InMemoryEntityCrudRepository<DocumentEntity, DocumentId>(), DocumentEntityRepository
