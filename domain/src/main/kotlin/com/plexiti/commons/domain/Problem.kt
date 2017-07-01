package com.plexiti.commons.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import java.util.*



/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class Problem: RuntimeException() {

    open val name = Name.default

    open val code = this::class.simpleName!!

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "CET")
    val occuredAt = Date()

}

internal class ProblemIntrospector: JacksonAnnotationIntrospector() {

    override fun hasIgnoreMarker(m: AnnotatedMember): Boolean {
        return m.getDeclaringClass().isAssignableFrom(RuntimeException::class.java) || super.hasIgnoreMarker(m)
    }

}
