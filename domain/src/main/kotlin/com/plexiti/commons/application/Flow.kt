package com.plexiti.commons.application

import com.plexiti.commons.domain.MessageType

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class Flow: Command() {

    override val type = MessageType.Flow

}
