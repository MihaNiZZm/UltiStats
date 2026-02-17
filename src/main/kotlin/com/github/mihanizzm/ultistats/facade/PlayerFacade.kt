package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.request.CreatePlayerRequest
import com.github.mihanizzm.ultistats.dto.response.PlayerResponse
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PlayerFacade(
    private val playerService: PlayerService,
    private val teamService: TeamService,
) {
    fun getAll(): List<PlayerResponse> =
        playerService.getAll().map { PlayerResponse.from(it) }

    fun getById(id: UUID): PlayerResponse? {
        val player = playerService.get(id) ?: return null
        return PlayerResponse.from(player)
    }

    fun create(request: CreatePlayerRequest): PlayerResponse {
        val playerId = UUID.randomUUID()
        val player = Player(
            id = playerId,
            teamId = request.teamId,
            number = request.number,
            firstName = request.firstName,
            lastName = request.lastName,
        )
        playerService.create(player)

        request.teamId?.let { teamId ->
            teamService.get(teamId)?.let { team ->
                val updatedTeam = team.copy(playerIds = team.playerIds + playerId)
                teamService.delete(teamId)
                teamService.create(updatedTeam)
            }
        }

        return PlayerResponse.from(player)
    }

    fun update(id: UUID, request: CreatePlayerRequest): PlayerResponse? {
        val existingPlayer = playerService.get(id) ?: return null
        val oldTeamId = existingPlayer.teamId
        val newTeamId = request.teamId

        val updatedPlayer = existingPlayer.copy(
            number = request.number,
            firstName = request.firstName,
            lastName = request.lastName,
            teamId = newTeamId,
        )
        playerService.update(updatedPlayer)

        if (oldTeamId != newTeamId) {
            oldTeamId?.let { oldId ->
                teamService.get(oldId)?.let { oldTeam ->
                    val updated = oldTeam.copy(playerIds = oldTeam.playerIds - id)
                    teamService.delete(oldId)
                    teamService.create(updated)
                }
            }
            newTeamId?.let { newId ->
                teamService.get(newId)?.let { newTeam ->
                    val updated = newTeam.copy(playerIds = newTeam.playerIds + id)
                    teamService.delete(newId)
                    teamService.create(updated)
                }
            }
        }

        return PlayerResponse.from(updatedPlayer)
    }

    fun delete(id: UUID): Boolean {
        val player = playerService.get(id) ?: return false

        player.teamId?.let { teamId ->
            teamService.get(teamId)?.let { team ->
                val updatedTeam = team.copy(playerIds = team.playerIds - id)
                teamService.delete(teamId)
                teamService.create(updatedTeam)
            }
        }

        playerService.delete(id)
        return true
    }
}
