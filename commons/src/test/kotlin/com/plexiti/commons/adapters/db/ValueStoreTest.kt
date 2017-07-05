package com.plexiti.commons.adapters.db

import com.plexiti.commons.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class ValueStoreTest {

    data class TestValue(val test: Int = 1): Value

    @Before
    fun prepare() {
        Value.store.deleteAll()
    }

    @Test
    fun empty () {
        assertThat(Value.store.findAll()).isEmpty()
    }

    @Test
    fun save() {
        Value.store.save(TestValue())
    }

    @Test
    fun findOne() {
        val value = Value.store.save(TestValue())
        val e = Value.store.findOne(Hash(value))
        assertThat(e).isEqualTo(value)
    }

    @Test
    fun findOne_Null() {
        Value.store.save(TestValue())
        val e = Value.store.findOne(Hash("anId"))
        assertThat(e).isNull()
    }

    @Test
    fun findOne_Json() {
        val expected = Value.store.save(TestValue())
        val actual = Value.store.findOne(expected.toJson())
        assertThat(actual).isEqualTo(expected)
    }

}
