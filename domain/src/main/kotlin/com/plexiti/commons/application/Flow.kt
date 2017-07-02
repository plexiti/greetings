package com.plexiti.commons.application

import com.plexiti.commons.domain.AggregateId
import com.plexiti.commons.domain.MessageType
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
