package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.PlayerFilterRequest
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataPlayerRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PlayerServiceImpl(
    private val playerRepository: SpringDataPlayerRepository,
) : PlayerService {
    private val log = LoggerFactory.getLogger(PlayerServiceImpl::class.java)

    override fun get(playerId: UUID): Player? = playerRepository.findByIdOrNull(playerId)

    override fun create(player: Player) {
        playerRepository.save(player)
    }

    override fun update(player: Player) {
        playerRepository.save(player)
    }

    override fun delete(playerId: UUID) = playerRepository.deleteById(playerId)

    override fun getAll(): List<Player> = playerRepository.findAll()

    override fun getAllByIds(ids: List<UUID>): List<Player> = playerRepository.findAllByIdIn(ids)

    override fun getAllByTeamId(teamId: UUID): List<Player> = playerRepository.findAllByTeamId(teamId)

    override fun findAllFiltered(filter: PlayerFilterRequest): List<Player> =
        playerRepository.findFiltered(filter.teamId, filter.name)

    override fun count(): Long = playerRepository.count()

    override fun countFiltered(filter: PlayerFilterRequest): Long =
        playerRepository.countFiltered(filter.teamId, filter.name)
}
