package com.plexiti.commons.application

import com.plexiti.commons.domain.*
import java.util.*
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Temporal
import javax.persistence.TemporalType

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class Result(): Message {

    override val type = MessageType.Result

    override lateinit var name: Name

    override lateinit var id: CommandId
        protected set

    lateinit var command: Command

    lateinit var execution: Execution

    lateinit var raised: List<Event>

    var document: Any? = null

    var problem: Problem? = null

    constructor(command: Command): this() {
        this.name = command.name
        this.id = command.id
        this.command = command
        this.execution = command.internals.execution
        this.raised = Event.repository.findByRaisedDuringOrderByRaisedAtDesc(command.id)
        this.document = Document.repository.findOne(command.internals.documentId)
        this.problem = command.internals.problem
    }

}

@Embeddable
class Execution() {

    @Column(name = "STARTED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var startedAt: Date? = null
        internal set

    @Column(name = "FINISHED_AT", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    var finishedAt: Date? = null
        internal set

}

