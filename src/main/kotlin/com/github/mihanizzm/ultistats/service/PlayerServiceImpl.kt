package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.Player
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PlayerServiceImpl(
    private val playerRepository: PlayerRepository,
) : PlayerService {
    private val log = LoggerFactory.getLogger(PlayerServiceImpl::class.java)

    override fun get(playerId: UUID): Player? = playerRepository.get(playerId)

    override fun create(player: Player) = playerRepository.save(player)

    override fun update(player: Player) = playerRepository.save(player)

    override fun delete(playerId: UUID) = playerRepository.delete(playerId)

    override fun getAll(): List<Player> = playerRepository.getAll()

    override fun getAllByIds(ids: List<UUID>): List<Player> = playerRepository.getAllByIds(ids)

    override fun getAllByTeamId(teamId: UUID): List<Player> = playerRepository.getAllByTeamId(teamId)
}
