package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.PlayerFilterRequest
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

    override fun findAllFiltered(filter: PlayerFilterRequest): List<Player> =
        players.values.filter { player -> matchesFilter(player, filter) }.toList()

    override fun count(): Long = players.size.toLong()

    override fun countFiltered(filter: PlayerFilterRequest): Long =
        players.values.count { player -> matchesFilter(player, filter) }.toLong()

    private fun matchesFilter(player: Player, filter: PlayerFilterRequest): Boolean {
        if (filter.teamId != null && player.teamId != filter.teamId) {
            return false
        }
        if (filter.name != null) {
            val searchName = filter.name.lowercase()
            val fullName = "${player.firstName} ${player.lastName}".lowercase()
            if (!fullName.contains(searchName)) {
                return false
            }
        }
        return true
    }
}
