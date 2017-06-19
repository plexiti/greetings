package com.plexiti.commons.adapters.db

import com.plexiti.commons.application.Command
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.application.CommandRepository
import com.plexiti.commons.domain.Event
import com.plexiti.commons.domain.EventId
import com.plexiti.commons.domain.EventRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */

@NoRepositoryBean
class InMemoryEventRepository: InMemoryEntityCrudRepository<Event, EventId>(), EventRepository {

    override fun findByAggregateId(id: String): List<Event> {
        return findAll().filter { id == it.aggregate.id }
    }

}

@NoRepositoryBean
class InMemoryCommandRepository: InMemoryEntityCrudRepository<Command, CommandId>(), CommandRepository
