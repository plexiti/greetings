package com.plexiti.commons.adapters.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.domain.*
import com.plexiti.commons.domain.ValueStore
import com.plexiti.utils.hash
import com.plexiti.utils.scanPackageForClassNames
import com.plexiti.utils.scanPackageForNamedClasses
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
class ValueStore : ValueStore<Value>, ApplicationContextAware {

    init { init() }

    private fun init() {
        types = scanPackageForNamedClasses("com.plexiti", Value::class)
        names = scanPackageForClassNames("com.plexiti", Value::class)
    }

    @org.springframework.beans.factory.annotation.Value("\${com.plexiti.app.context}")
    private var context = Name.context

    @Autowired
    private var delegate: StoredValueStore = InMemoryStoredValueStore()

    lateinit internal var types: Map<Name, KClass<out Value>>
    lateinit internal var names: Map<KClass<out Value>, Name>

    internal fun type(qName: Name): KClass<out Value> {
        return types.get(qName) ?: throw IllegalArgumentException("Value type '$qName' is not mapped to a local object type!")
    }

    private fun toValue(stored: StoredValue?): Value? {
        return  if (stored != null) Value.fromJson(stored.json, type(stored.name)) else null
    }

    private fun toEntity(value: Value?): StoredValue? {
        if (value != null) {
            val text = ObjectMapper().writeValueAsString(value)
            val id = Hash(value)
            return delegate.findOne(id) ?: StoredValue(id, value.name, text)
        }
        return null
    }

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Value.store = this
        Name.context = context; init()
    }

    override fun exists(id: Hash?): Boolean {
        return delegate.exists(id)
    }

    override fun findOne(id: Hash?): Value? {
        return toValue(delegate.findOne(id))
    }

    fun valueId(json: String?): Hash? {
        return if (json != null) Hash(hash(json)) else null
    }

    fun findOne(json: String): Value? {
        return findOne(valueId(json))
    }

    override fun findAll(): MutableIterable<Value> {
        return delegate.findAll().mapTo(ArrayList(), { toValue(it)!! })
    }

    override fun findAll(ids: MutableIterable<Hash>?): MutableIterable<Value> {
        return delegate.findAll(ids).mapTo(ArrayList(), { toValue(it)!! })
    }

    override fun <S : Value?> save(value: S): S {
        @Suppress("unchecked_cast")
        return toValue(delegate.save(toEntity(value))) as S
    }

    override fun <S : Value?> save(values: MutableIterable<S>?): MutableIterable<S> {
        return values?.mapTo(ArrayList(), { save(it) })!!
    }

    override fun count(): Long {
        return delegate.count()
    }

    override fun delete(entities: MutableIterable<Value>?) {
        delegate.delete(entities?.map { toEntity(it) })
    }

    override fun delete(value: Value?) {
        delegate.delete(toEntity(value))
    }

    override fun delete(id: Hash?) {
        delegate.delete(id)
    }

    override fun deleteAll() {
        delegate.deleteAll()
    }

}

@Repository
internal interface StoredValueStore : ValueStore<StoredValue>

@NoRepositoryBean
class InMemoryStoredValueStore : InMemoryEntityCrudRepository<StoredValue, Hash>(), StoredValueStore
