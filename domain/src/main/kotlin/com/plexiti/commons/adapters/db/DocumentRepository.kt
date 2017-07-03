package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.application.Document
import com.plexiti.commons.application.DocumentEntity
import com.plexiti.commons.application.DocumentId
import com.plexiti.commons.application.DocumentRepository
import com.plexiti.utils.hash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component @NoRepositoryBean
class DocumentRepository: DocumentRepository<Any>, ApplicationContextAware {

    @Autowired
    private var delegate: DocumentEntityRepository = InMemoryDocumentEntityRepository()

    private fun toDocument(entity: DocumentEntity?): Any? {
        return  if (entity != null) Document.fromJson(entity.json, entity.type) else null
    }

    private fun toEntity(document: Any?): DocumentEntity? {
        if (document != null) {
            val text = ObjectMapper().writeValueAsString(document)
            val id = DocumentId(hash(text))
            return delegate.findOne(id) ?: DocumentEntity(id, document::class, text)
        }
        return null
    }

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Document.store = this
    }

    override fun exists(id: DocumentId?): Boolean {
        return delegate.exists(id)
    }

    override fun findOne(id: DocumentId?): Any? {
        return toDocument(delegate.findOne(id))
    }

    fun documentId(json: String?): DocumentId? {
        return if (json != null) DocumentId(hash(json)) else null
    }

    fun findOne(json: String): Any? {
        return findOne(documentId(json))
    }

    override fun findAll(): MutableIterable<Any> {
        return delegate.findAll().mapTo(ArrayList(), { toDocument(it)!! })
    }

    override fun findAll(ids: MutableIterable<DocumentId>?): MutableIterable<Any> {
        return delegate.findAll(ids).mapTo(ArrayList(), { toDocument(it)!! })
    }

    override fun <S : Any?> save(any: S): S {
        @Suppress("unchecked_cast")
        return toDocument(delegate.save(toEntity(any))) as S
    }

    override fun <S : Any?> save(anys: MutableIterable<S>?): MutableIterable<S> {
        return anys?.mapTo(ArrayList(), { save(it) })!!
    }

    override fun count(): Long {
        return delegate.count()
    }

    override fun delete(entities: MutableIterable<Any>?) {
        delegate.delete(entities?.map { toEntity(it) })
    }

    override fun delete(any: Any?) {
        delegate.delete(toEntity(any))
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
