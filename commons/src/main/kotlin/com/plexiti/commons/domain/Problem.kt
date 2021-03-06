package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import java.util.*
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Temporal
import javax.persistence.TemporalType


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Embeddable
@JsonInclude(NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
open class Problem(): RuntimeException() {

    @Column(name = "PROBLEM_OCCURED_CODE", nullable = true, length = 128)
    var code = this::class.java.simpleName
        protected set

    @Column(name = "PROBLEM_OCCURED_MESSAGE", nullable = true, length = 1024)
    override var message: String? = null
        protected set

    @Column(name = "PROBLEM_OCCURED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    val occuredAt = Date()

    constructor(code: String, message: String? = null): this() {
        this.code = code
        this.message = message
    }

    fun toJson(): String {
        return ObjectMapper().setAnnotationIntrospector(ProblemIntrospector()).writeValueAsString(this)
    }

}

internal class ProblemIntrospector: JacksonAnnotationIntrospector() {

    override fun hasIgnoreMarker(m: AnnotatedMember): Boolean {
        return m.getDeclaringClass().isAssignableFrom(RuntimeException::class.java) || super.hasIgnoreMarker(m)
    }

}


class ProblemSerializer : JsonSerializer<Problem>() {

    override fun serialize(problem: Problem, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
        jsonGenerator.writeTree(ObjectMapper().readTree(problem.toJson()))
    }

}
