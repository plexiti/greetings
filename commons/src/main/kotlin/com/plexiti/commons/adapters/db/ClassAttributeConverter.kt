package com.plexiti.commons.adapters.db

import javax.persistence.Converter
import kotlin.reflect.KClass


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Converter
class ClassAttributeConverter: javax.persistence.AttributeConverter<Class<*>, String> {

    override fun convertToDatabaseColumn(cls: Class<*>): String {
        return cls.name
    }

    override fun convertToEntityAttribute(className: String): Class<*> {
        return Class.forName(className)
    }

}

@Converter
class KClassAttributeConverter: javax.persistence.AttributeConverter<KClass<*>, String> {

    override fun convertToDatabaseColumn(cls: KClass<*>): String {
        return cls.java.name
    }

    override fun convertToEntityAttribute(className: String): KClass<*> {
        return Class.forName(className).kotlin
    }

}
