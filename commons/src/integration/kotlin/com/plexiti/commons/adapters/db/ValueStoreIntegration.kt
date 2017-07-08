package com.plexiti.commons.adapters.db

import com.plexiti.commons.DataJpaIntegration
import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class ValueStoreIntegration: DataJpaIntegration() {

    @org.springframework.beans.factory.annotation.Autowired
    internal lateinit var eventRepository: com.plexiti.commons.adapters.db.EventStore

    data class ITValue(val test: Int = 1): Value

    @Test
    fun empty () {
        assertThat(Value.store.findAll()).isEmpty()
    }

    @Test
    fun save() {
        Value.store.save(ITValue())
    }

    @Test
    fun findOne() {
        val value = Value.store.save(ITValue())
        val e = Value.store.findOne(Hash(value))
        assertThat(e).isEqualTo(value)
    }

    @Test
    fun findOne_Null() {
        Value.store.save(ITValue())
        val e = Value.store.findOne(Hash("anId"))
        assertThat(e).isNull()
    }

    @Test
    fun findOne_Json() {
        val expected = Value.store.save(ITValue())
        val actual = Value.store.findOne(expected.toJson())
        assertThat(actual).isEqualTo(expected)
    }

}
