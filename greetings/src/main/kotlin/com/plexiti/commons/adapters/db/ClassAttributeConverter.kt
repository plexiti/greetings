package com.plexiti.commons.adapters.db

import javax.persistence.Converter


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
