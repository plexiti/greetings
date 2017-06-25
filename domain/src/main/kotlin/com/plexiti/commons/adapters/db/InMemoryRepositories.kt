package com.plexiti.commons.adapters.db

import com.plexiti.commons.application.CommandEntity
import com.plexiti.commons.application.CommandId
import com.plexiti.commons.application.CorrelationKey
import com.plexiti.commons.domain.EventEntity
import com.plexiti.commons.domain.EventId
import org.springframework.data.repository.NoRepositoryBean

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */

@NoRepositoryBean
class InMemoryEventEntityRepository: InMemoryEntityCrudRepository<EventEntity, EventId>(), EventEntityRepository {

    override fun findByAggregateId(id: String): List<EventEntity> {
        return findAll().filter { id == it.aggregate.id }
    }

}

@NoRepositoryBean
class InMemoryCommandEntityRepository: InMemoryEntityCrudRepository<CommandEntity, CommandId>(), CommandEntityRepository {

    override fun findByFinishKey(finishKey: CorrelationKey): CommandEntity? {
        return findAll().find { finishKey == it.finishKey }
    }

}
