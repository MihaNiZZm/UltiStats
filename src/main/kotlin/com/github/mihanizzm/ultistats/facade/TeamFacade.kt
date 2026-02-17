package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.request.CreatePlayerRequest
import com.github.mihanizzm.ultistats.dto.request.CreateTeamRequest
import com.github.mihanizzm.ultistats.dto.response.PlayerResponse
import com.github.mihanizzm.ultistats.dto.response.TeamResponse
import com.github.mihanizzm.ultistats.model.Player
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

        // Создаем игроков и собираем их ID
        val playerIds = request.players.map { playerRequest ->
            val player = playerRequest.toPlayer(teamId)
            playerService.create(player)
            player.id
        }

        val team = Team(
            id = teamId,
            name = request.name,
            playerIds = playerIds,
        )
        teamService.create(team)

        val players = playerService.getAllByIds(playerIds)
        return TeamResponse.from(team, players)
    }

    fun update(id: UUID, request: CreateTeamRequest): TeamResponse? {
        val existingTeam = teamService.get(id) ?: return null

        // Удаляем старых игроков из команды (обнуляем их teamId)
        existingTeam.playerIds.forEach { playerId ->
            playerService.get(playerId)?.let { player ->
                playerService.update(player.copy(teamId = null))
            }
        }

        // Создаем новых игроков
        val playerIds = request.players.map { playerRequest ->
            val player = playerRequest.toPlayer(id)
            playerService.create(player)
            player.id
        }

        val updatedTeam = existingTeam.copy(
            name = request.name,
            playerIds = playerIds,
        )
        teamService.delete(id)
        teamService.create(updatedTeam)

        val players = playerService.getAllByIds(playerIds)
        return TeamResponse.from(updatedTeam, players)
    }

    fun delete(id: UUID): Boolean {
        val team = teamService.get(id) ?: return false

        // Обнуляем teamId у игроков команды
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

        // Если игрок уже в этой команде
        if (team.hasPlayer(playerId)) {
            val players = playerService.getAllByIds(team.playerIds)
            return TeamResponse.from(team, players)
        }

        // Удаляем из старой команды если был
        player.teamId?.let { oldTeamId ->
            if (oldTeamId != teamId) {
                teamService.get(oldTeamId)?.let { oldTeam ->
                    val updatedOldTeam = oldTeam.copy(playerIds = oldTeam.playerIds - playerId)
                    teamService.delete(oldTeamId)
                    teamService.create(updatedOldTeam)
                }
            }
        }

        // Обновляем teamId у игрока
        playerService.update(player.copy(teamId = teamId))

        // Добавляем в новую команду
        val updatedTeam = team.copy(playerIds = team.playerIds + playerId)
        teamService.delete(teamId)
        teamService.create(updatedTeam)

        val players = playerService.getAllByIds(updatedTeam.playerIds)
        return TeamResponse.from(updatedTeam, players)
    }

    fun removePlayerFromTeam(teamId: UUID, playerId: UUID): TeamResponse? {
        val team = teamService.get(teamId) ?: return null
        if (!team.hasPlayer(playerId)) return null

        // Обновляем teamId у игрока (убираем из команды)
        playerService.get(playerId)?.let { player ->
            playerService.update(player.copy(teamId = null))
        }

        val updatedTeam = team.copy(playerIds = team.playerIds - playerId)
        teamService.delete(teamId)
        teamService.create(updatedTeam)

        val players = playerService.getAllByIds(updatedTeam.playerIds)
        return TeamResponse.from(updatedTeam, players)
    }

    private fun CreatePlayerRequest.toPlayer(teamId: UUID) = Player(
        id = UUID.randomUUID(),
        teamId = teamId,
        number = number,
        firstName = firstName,
        lastName = lastName,
    )
}
