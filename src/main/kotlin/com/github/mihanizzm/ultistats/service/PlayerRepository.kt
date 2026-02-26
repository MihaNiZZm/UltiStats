package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.PlayerFilterRequest
import com.github.mihanizzm.ultistats.model.Player
import java.util.UUID

interface PlayerRepository {
    fun get(id: UUID): Player?

    fun save(player: Player)

    fun delete(id: UUID)

    fun getAll(): List<Player>

    fun getAllByIds(ids: List<UUID>): List<Player>

    fun getAllByTeamId(teamId: UUID): List<Player>

    fun findAllFiltered(filter: PlayerFilterRequest): List<Player>

    fun count(): Long

    fun countFiltered(filter: PlayerFilterRequest): Long
}