package com.plexiti.commons.adapters.db

import com.plexiti.commons.application.CommandEntity
import com.plexiti.commons.application.CommandEntityRepository
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.domain.*
import org.springframework.data.repository.NoRepositoryBean

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */

@NoRepositoryBean
class InMemoryEventRepository: InMemoryEntityCrudRepository<EventEntity, EventId>(), EventEntityRepository {

    override fun findByAggregateId(id: String): List<EventEntity> {
        return findAll().filter { id == it.aggregate.id }
    }

}

@NoRepositoryBean
class InMemoryCommandRepository: InMemoryEntityCrudRepository<CommandEntity, CommandId>(), CommandEntityRepository
