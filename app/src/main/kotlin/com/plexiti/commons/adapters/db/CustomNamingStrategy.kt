package com.plexiti.commons.adapters.db

import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class CustomNamingStrategy : PhysicalNamingStrategyStandardImpl() {

    val PREFIX = "GRT_"

    override fun toPhysicalTableName(name: Identifier?, context: JdbcEnvironment?): Identifier {
        return Identifier.toIdentifier(PREFIX + super.toPhysicalTableName(name, context).text)
    }

    override fun toPhysicalSequenceName(name: Identifier?, context: JdbcEnvironment?): Identifier {
        return Identifier.toIdentifier(PREFIX + super.toPhysicalSequenceName(name, context).text)
    }

}
