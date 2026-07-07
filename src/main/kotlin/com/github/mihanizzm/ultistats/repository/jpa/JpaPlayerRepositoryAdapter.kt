package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.dto.request.PlayerFilterRequest
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.repository.mapper.toDomain
import com.github.mihanizzm.ultistats.repository.mapper.toEntity
import com.github.mihanizzm.ultistats.service.PlayerRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaPlayerRepositoryAdapter(
    private val springDataPlayerRepository: SpringDataPlayerRepository,
) : PlayerRepository {

    override fun get(id: UUID): Player? =
        springDataPlayerRepository.findByIdOrNull(id)?.toDomain()

    override fun save(player: Player) {
        springDataPlayerRepository.save(player.toEntity())
    }

    override fun delete(id: UUID) {
        springDataPlayerRepository.deleteById(id)
    }

    override fun getAll(): List<Player> =
        springDataPlayerRepository.findAll().map { it.toDomain() }

    override fun getAllByIds(ids: List<UUID>): List<Player> =
        springDataPlayerRepository.findAllByIdIn(ids).map { it.toDomain() }

    override fun getAllByTeamId(teamId: UUID): List<Player> =
        springDataPlayerRepository.findAllByTeamId(teamId).map { it.toDomain() }

    override fun findAllFiltered(filter: PlayerFilterRequest): List<Player> =
        springDataPlayerRepository.findFiltered(filter.teamId, filter.name).map { it.toDomain() }

    override fun count(): Long = springDataPlayerRepository.count()

    override fun countFiltered(filter: PlayerFilterRequest): Long =
        springDataPlayerRepository.countFiltered(filter.teamId, filter.name)
}
