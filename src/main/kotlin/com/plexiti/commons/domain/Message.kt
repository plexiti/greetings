package com.plexiti.commons.domain

import javax.persistence.*

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@MappedSuperclass
abstract class AbstractMessageEntity<ID: MessageId>: AbstractEntity<ID>()

@MappedSuperclass
open class MessageId(value: String): AggregateId(value)
