package com.plexiti.commons.application

import com.plexiti.commons.domain.AggregateId
import com.plexiti.commons.domain.MessageType
import com.plexiti.commons.domain.Name
import java.util.*
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class Flow: Command() {
    override val type = MessageType.Flow
}

@Entity
@DiscriminatorValue(MessageType.Discriminator.flow)
class FlowEntity(): CommandEntity()

open class FlowId(value: String = ""): CommandId(value)

open class TokenId(value: String = ""): AggregateId(value)

class FlowCommand(): Command() {

    lateinit var flowId: FlowId
    lateinit var tokenId: TokenId

    constructor(name: Name, flowId: FlowId, tokenId: TokenId, issuedAt: Date = Date()): this() {
        this.id = CommandId(UUID.randomUUID().toString())
        this.name = name
        this.flowId = flowId
        this.tokenId = tokenId
        this.issuedAt = issuedAt
    }

}
