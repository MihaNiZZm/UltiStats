package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.PlayerFilterRequest
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataPlayerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class PlayerServiceImpl(
    private val playerRepository: SpringDataPlayerRepository,
    private val teamPlayerService: TeamPlayerService,
) : PlayerService {
    private val log = LoggerFactory.getLogger(PlayerServiceImpl::class.java)

    override fun get(playerId: UUID): Player? =
        playerRepository.findByIdAndDeletedAtIsNull(playerId)?.withPrimaryMembership()

    override fun create(player: Player) {
        playerRepository.save(player)
        player.teamId?.let { teamPlayerService.add(it, player.id, player.number) }
    }

    override fun update(player: Player) {
        playerRepository.save(player)
    }

    override fun delete(playerId: UUID) {
        get(playerId)?.let { playerRepository.save(it.copy(deletedAt = Instant.now())) }
    }

    override fun getAll(): List<Player> = playerRepository.findAllByDeletedAtIsNull().map { it.withPrimaryMembership() }

    override fun getAllByIds(ids: List<UUID>): List<Player> =
        playerRepository.findAllByIdInAndDeletedAtIsNull(ids).map { it.withPrimaryMembership() }

    override fun getAllByTeamId(teamId: UUID): List<Player> {
        val memberships = teamPlayerService.getByTeamId(teamId).associateBy { it.playerId }
        return playerRepository.findAllByIdInAndDeletedAtIsNull(memberships.keys.toList()).map { player ->
            player.copy(teamId = teamId, number = memberships[player.id]?.number)
        }
    }

    override fun findAllFiltered(filter: PlayerFilterRequest): List<Player> {
        val players = playerRepository.findFiltered(filter.name)
        if (filter.teamId == null) return players.map { it.withPrimaryMembership() }
        val memberships = teamPlayerService.getByTeamId(filter.teamId).associateBy { it.playerId }
        return players.filter { it.id in memberships }.map {
            it.copy(teamId = filter.teamId, number = memberships[it.id]?.number)
        }
    }

    override fun count(): Long = playerRepository.countByDeletedAtIsNull()

    override fun countFiltered(filter: PlayerFilterRequest): Long =
        findAllFiltered(filter).size.toLong()

    private fun Player.withPrimaryMembership(): Player {
        val membership = teamPlayerService.getByPlayerId(id).firstOrNull() ?: return this
        return copy(teamId = membership.teamId, number = membership.number)
    }
}
