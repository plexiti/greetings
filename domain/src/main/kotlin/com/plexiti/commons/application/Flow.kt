package com.plexiti.commons.application

import com.plexiti.commons.domain.AggregateId
import com.plexiti.commons.domain.MessageType
import com.plexiti.commons.domain.Name
import java.util.*
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class Flow: Command() {
    override val type = MessageType.Flow
}

@Entity
@DiscriminatorValue(MessageType.Discriminator.flow)
@NamedQueries(
    NamedQuery(
        name = "FlowForwarder",
        query = "select f from FlowEntity f where f.forwardedAt is null"
    )
)
class FlowEntity(): CommandEntity()

open class TokenId(value: String = ""): AggregateId(value)

class FlowCommand(): Command() {

    lateinit var tokenId: TokenId

    constructor(name: Name, tokenId: TokenId, issuedAt: Date = Date()): this() {
        this.id = CommandId(UUID.randomUUID().toString())
        this.name = name
        this.tokenId = tokenId
        this.issuedAt = issuedAt
    }

}
