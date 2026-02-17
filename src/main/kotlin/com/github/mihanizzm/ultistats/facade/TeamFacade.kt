package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.request.CreateTeamRequest
import com.github.mihanizzm.ultistats.dto.response.TeamResponse
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TeamFacade(
    private val teamService: TeamService,
    private val playerService: PlayerService,
) {
    fun getAll(): List<TeamResponse> =
        teamService.getAll().map { team ->
            val players = playerService.getAllByIds(team.playerIds)
            TeamResponse.from(team, players)
        }

    fun getById(id: UUID): TeamResponse? {
        val team = teamService.get(id) ?: return null
        val players = playerService.getAllByIds(team.playerIds)
        return TeamResponse.from(team, players)
    }

    fun create(request: CreateTeamRequest): TeamResponse {
        val teamId = UUID.randomUUID()

        request.playerIds.forEach { playerId ->
            playerService.get(playerId)?.let { player ->
                playerService.update(player.copy(teamId = teamId))
            }
        }

        val team = Team(
            id = teamId,
            name = request.name,
            playerIds = request.playerIds,
        )
        teamService.create(team)

        val players = playerService.getAllByIds(request.playerIds)
        return TeamResponse.from(team, players)
    }

    fun update(id: UUID, request: CreateTeamRequest): TeamResponse? {
        val existingTeam = teamService.get(id) ?: return null

        existingTeam.playerIds.forEach { playerId ->
            playerService.get(playerId)?.let { player ->
                playerService.update(player.copy(teamId = null))
            }
        }

        request.playerIds.forEach { playerId ->
            playerService.get(playerId)?.let { player ->
                playerService.update(player.copy(teamId = id))
            }
        }

        val updatedTeam = existingTeam.copy(
            name = request.name,
            playerIds = request.playerIds,
        )
        teamService.delete(id)
        teamService.create(updatedTeam)

        val players = playerService.getAllByIds(request.playerIds)
        return TeamResponse.from(updatedTeam, players)
    }

    fun delete(id: UUID): Boolean {
        val team = teamService.get(id) ?: return false

        team.playerIds.forEach { playerId ->
            playerService.get(playerId)?.let { player ->
                playerService.update(player.copy(teamId = null))
            }
        }

        teamService.delete(id)
        return true
    }

    fun addPlayerToTeam(teamId: UUID, playerId: UUID): TeamResponse? {
        val team = teamService.get(teamId) ?: return null
        val player = playerService.get(playerId) ?: return null

        if (team.hasPlayer(playerId)) {
            val players = playerService.getAllByIds(team.playerIds)
            return TeamResponse.from(team, players)
        }

        player.teamId?.let { oldTeamId ->
            if (oldTeamId != teamId) {
                teamService.get(oldTeamId)?.let { oldTeam ->
                    val updatedOldTeam = oldTeam.copy(playerIds = oldTeam.playerIds - playerId)
                    teamService.delete(oldTeamId)
                    teamService.create(updatedOldTeam)
                }
            }
        }

        playerService.update(player.copy(teamId = teamId))

        val updatedTeam = team.copy(playerIds = team.playerIds + playerId)
        teamService.delete(teamId)
        teamService.create(updatedTeam)

        val players = playerService.getAllByIds(updatedTeam.playerIds)
        return TeamResponse.from(updatedTeam, players)
    }

    fun removePlayerFromTeam(teamId: UUID, playerId: UUID): TeamResponse? {
        val team = teamService.get(teamId) ?: return null
        if (!team.hasPlayer(playerId)) return null

        playerService.get(playerId)?.let { player ->
            playerService.update(player.copy(teamId = null))
        }

        val updatedTeam = team.copy(playerIds = team.playerIds - playerId)
        teamService.delete(teamId)
        teamService.create(updatedTeam)

        val players = playerService.getAllByIds(updatedTeam.playerIds)
        return TeamResponse.from(updatedTeam, players)
    }
}
