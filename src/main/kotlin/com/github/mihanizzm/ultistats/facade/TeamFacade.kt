package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.request.CreatePlayerRequest
import com.github.mihanizzm.ultistats.dto.request.CreateTeamRequest
import com.github.mihanizzm.ultistats.dto.response.TeamResponse
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.service.TeamService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TeamFacade(
    private val teamService: TeamService,
) {
    fun getAll(): List<TeamResponse> =
        teamService.getAll().map { TeamResponse.from(it) }

    fun getById(id: UUID): TeamResponse? {
        val team = teamService.get(id) ?: return null
        return TeamResponse.from(team)
    }

    fun create(request: CreateTeamRequest): TeamResponse {
        val teamId = UUID.randomUUID()
        val players = request.players.map { it.toPlayer(teamId) }
        val team = Team(
            id = teamId,
            name = request.name,
            players = players,
        )
        teamService.create(team)
        return TeamResponse.from(team)
    }

    fun update(id: UUID, request: CreateTeamRequest): TeamResponse? {
        val existingTeam = teamService.get(id) ?: return null
        val players = request.players.map { it.toPlayer(id) }
        val updatedTeam = existingTeam.copy(
            name = request.name,
            players = players,
        )
        teamService.delete(id)
        teamService.create(updatedTeam)
        return TeamResponse.from(updatedTeam)
    }

    fun delete(id: UUID): Boolean {
        if (teamService.get(id) == null) return false
        teamService.delete(id)
        return true
    }

    fun addPlayer(teamId: UUID, request: CreatePlayerRequest): TeamResponse? {
        val team = teamService.get(teamId) ?: return null
        val newPlayer = request.toPlayer(teamId)
        val updatedTeam = team.copy(players = team.players + newPlayer)
        teamService.delete(teamId)
        teamService.create(updatedTeam)
        return TeamResponse.from(updatedTeam)
    }

    fun removePlayer(teamId: UUID, playerId: UUID): TeamResponse? {
        val team = teamService.get(teamId) ?: return null
        if (!team.hasPlayer(playerId)) return null
        val updatedTeam = team.copy(players = team.players.filter { it.id != playerId })
        teamService.delete(teamId)
        teamService.create(updatedTeam)
        return TeamResponse.from(updatedTeam)
    }

    private fun CreatePlayerRequest.toPlayer(teamId: UUID) = Player(
        id = UUID.randomUUID(),
        teamId = teamId,
        number = number,
        firstName = firstName,
        lastName = lastName,
    )
}
