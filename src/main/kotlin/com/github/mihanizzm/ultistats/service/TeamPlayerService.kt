package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.TeamPlayer
import java.util.UUID

interface TeamPlayerService {
    fun get(teamId: UUID, playerId: UUID): TeamPlayer?
    fun getByTeamId(teamId: UUID): List<TeamPlayer>
    fun getByPlayerId(playerId: UUID): List<TeamPlayer>
    fun add(teamId: UUID, playerId: UUID, number: Int? = null): TeamPlayer
    fun remove(teamId: UUID, playerId: UUID): Boolean
}
