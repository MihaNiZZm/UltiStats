package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.Player
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Repository
class PlayerRepositoryConcurrentMapImpl : PlayerRepository {
    private val players = ConcurrentHashMap<UUID, Player>()

    override fun get(id: UUID): Player? = players[id]

    override fun save(player: Player) {
        players[player.id] = player
    }

    override fun delete(id: UUID) {
        players.remove(id)
    }

    override fun getAll(): List<Player> = players.values.toList()

    override fun getAllByIds(ids: List<UUID>): List<Player> = ids.mapNotNull { get(it) }

    override fun getAllByTeamId(teamId: UUID): List<Player> =
        players.values.filter { it.teamId == teamId }.toList()
}
