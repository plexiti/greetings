package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonFormat
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
open class Problem: RuntimeException() {

    @Column(name = "PROBLEM_CODE", nullable = true)
    open val code = this::class.simpleName!!

    @Column(name = "PROBLEM_OCCURED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    val occuredAt = Date()

}

internal class ProblemIntrospector: JacksonAnnotationIntrospector() {

    override fun hasIgnoreMarker(m: AnnotatedMember): Boolean {
        return m.getDeclaringClass().isAssignableFrom(RuntimeException::class.java) || super.hasIgnoreMarker(m)
    }

}
