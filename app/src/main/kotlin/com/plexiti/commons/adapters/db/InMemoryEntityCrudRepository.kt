package com.plexiti.commons.adapters.db

import com.plexiti.commons.domain.AbstractEntity
import org.springframework.data.repository.CrudRepository
import java.io.Serializable

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class InMemoryEntityCrudRepository<T: AbstractEntity<ID>, ID: Serializable>: CrudRepository<T, ID> {

    val map = HashMap<ID, T>()

    override fun delete(entity: T) {
        delete(entity.id)
    }

    override fun delete(entities: MutableIterable<T>?) {
        entities?.forEach { delete(it) }
    }

    override fun delete(id: ID) {
        map.remove(id)
    }

    override fun findAll(): MutableIterable<T> {
        return map.values
    }

    override fun findAll(ids: MutableIterable<ID>?): MutableIterable<T> {
        return if (ids != null) findAll().filter { ids.contains(it.id) }.toMutableList() else ArrayList()
    }

    override fun findOne(id: ID): T? {
        return findAll().find { id == it.id }
    }

    override fun count(): Long {
        return map.size.toLong()
    }

    override fun deleteAll() {
        map.clear()
    }

    override fun exists(id: ID): Boolean {
        return map.containsKey(id)
    }

    override fun <S : T> save(entities: MutableIterable<S>?): MutableIterable<S> {
        entities?.forEach { save(it) }
        return entities ?: ArrayList();
    }

    override fun <S : T> save(entity: S): S {
        map.put(entity.id, entity)
        return entity
    }

}
