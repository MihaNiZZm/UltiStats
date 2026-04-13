package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.dto.request.TeamFilterRequest
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.repository.mapper.toDomain
import com.github.mihanizzm.ultistats.repository.mapper.toEntity
import com.github.mihanizzm.ultistats.service.TeamRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
@Profile("postgres")
class JpaTeamRepositoryAdapter(
    private val springDataTeamRepository: SpringDataTeamRepository,
    private val springDataPlayerRepository: SpringDataPlayerRepository,
) : TeamRepository {

    override fun get(id: UUID): Team? {
        val entity = springDataTeamRepository.findByIdOrNull(id) ?: return null
        val playerIds = springDataPlayerRepository.findAllByTeamId(id).map { it.id }
        return entity.toDomain(playerIds)
    }

    override fun save(team: Team) {
        springDataTeamRepository.save(team.toEntity())
    }

    override fun delete(id: UUID) {
        springDataTeamRepository.deleteById(id)
    }

    override fun getAll(): List<Team> {
        return springDataTeamRepository.findAll().map { entity ->
            val playerIds = springDataPlayerRepository.findAllByTeamId(entity.id).map { it.id }
            entity.toDomain(playerIds)
        }
    }

    override fun getAllInList(ids: List<UUID>): List<Team> {
        return springDataTeamRepository.findAllByIdIn(ids).map { entity ->
            val playerIds = springDataPlayerRepository.findAllByTeamId(entity.id).map { it.id }
            entity.toDomain(playerIds)
        }
    }

    override fun findAllFiltered(filter: TeamFilterRequest): List<Team> {
        val entities = if (filter.name != null) {
            springDataTeamRepository.findByNameContainingIgnoreCase(filter.name)
        } else {
            springDataTeamRepository.findAll()
        }
        return entities.map { entity ->
            val playerIds = springDataPlayerRepository.findAllByTeamId(entity.id).map { it.id }
            entity.toDomain(playerIds)
        }
    }

    override fun count(): Long = springDataTeamRepository.count()

    override fun countFiltered(filter: TeamFilterRequest): Long {
        return if (filter.name != null) {
            springDataTeamRepository.countByNameContainingIgnoreCase(filter.name)
        } else {
            springDataTeamRepository.count()
        }
    }
}
