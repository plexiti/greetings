package com.plexiti.commons.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.databind.ObjectMapper
import com.plexiti.commons.domain.*
import java.util.*
import javax.persistence.*
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class Flow(): Command() {

    override val type = MessageType.Flow

    constructor(name: Name): this() {

        this.name = name

    }

    override fun trigger(event: Event): Flow? {

        var flow: Flow? = null
        triggers.forEach { eventName, flowName ->
            if (this.name == flowName && event.name == eventName)
                flow = Flow(flowName)
        }
        return flow

    }

    companion object {

        var triggers: Map<Name, Name> = emptyMap()

        private val executing = ThreadLocal<StoredFlow?>()

        internal fun setExecuting(flow: Flow) {
            setExecuting(flow.internals() as StoredFlow)
        }

        internal fun setExecuting(flow: StoredFlow?) {
            executing.set(flow)
        }

        internal fun getExecuting(): StoredFlow? {
            return executing.get()
        }

        internal fun unsetExecuting() {
            executing.set(null)
        }

    }

}

@Entity
@DiscriminatorValue(MessageType.Discriminator.flow)
@NamedQueries(
    NamedQuery(
        name = "FlowQueuer",
        query = "select f from StoredFlow f where f.forwardedAt is null"
    )
)
class StoredFlow: StoredCommand {

    @Transient
    override val type = MessageType.Flow

    constructor() : super()
    constructor(flow: Flow) : super(flow)

    fun resume() {
        if (this.execution == null) {
            this.status = CommandStatus.started
            this.execution = Execution()
            this.execution?.startedAt = Date()
        }
        Flow.setExecuting(this)
    }

    fun hibernate() {
        Flow.unsetExecuting()
    }

}

open class TokenId(value: String = ""): AggregateId(value)

@JsonInclude(NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
class FlowIO() {

    lateinit var type: MessageType

    var command: Command? = null
    var event: Event? = null
    var document: Document? = null

    lateinit var flowId: CommandId
    var tokenId: TokenId? = null

    constructor(event: Event, flowId: CommandId): this() {
        this.type = event.type
        this.event = event
        this.flowId = flowId
    }

    constructor(command: Command, flowId: CommandId, tokenId: TokenId? = null): this() {
        this.type = command.type
        this.command = command
        this.flowId = flowId
        this.tokenId = tokenId
    }

    constructor(document: Document, flowId: CommandId, tokenId: TokenId? = null): this() {
        this.type = document.type
        this.document = document
        this.flowId = flowId
        this.tokenId = tokenId
    }

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

    companion object {

        fun fromJson(json: String): FlowIO {
            return ObjectMapper().readValue(json, FlowIO::class.java)
        }

    }

}
