package com.plexiti.commons.domain

import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@MappedSuperclass
abstract class AbstractMessageEntity<ID: MessageId>(id: ID? = null): AbstractEntity<ID>(id)

@MappedSuperclass
open class MessageId(value: String? = null): AggregateId(value)
