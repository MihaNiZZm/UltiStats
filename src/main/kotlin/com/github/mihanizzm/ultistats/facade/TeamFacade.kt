package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.common.PageResponse
import com.github.mihanizzm.ultistats.dto.common.SortParam
import com.github.mihanizzm.ultistats.dto.request.CreateTeamRequest
import com.github.mihanizzm.ultistats.dto.request.TeamFilterRequest
import com.github.mihanizzm.ultistats.dto.request.UpdateTeamRequest
import com.github.mihanizzm.ultistats.dto.response.PhotoUrlResponse
import com.github.mihanizzm.ultistats.dto.response.TeamDetailResponse
import com.github.mihanizzm.ultistats.dto.response.TeamListItemResponse
import com.github.mihanizzm.ultistats.dto.response.TeamResponse
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.service.LocalFileStorageService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import com.github.mihanizzm.ultistats.util.SortingUtils.applySorting
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Component
class TeamFacade(
    private val teamService: TeamService,
    private val playerService: PlayerService,
    private val localFileStorageService: LocalFileStorageService,
) {
    companion object {
        val DEFAULT_SORT = SortParam("name")

        val SORT_FIELD_EXTRACTORS: Map<String, (Team) -> Comparable<*>?> = mapOf(
            "name" to { it.name },
        )
    }

    fun getAll(): List<TeamResponse> =
        teamService.getAll().map { team ->
            val players = playerService.getAllByIds(team.playerIds)
            TeamResponse.from(team, players)
        }

    fun getAllPaged(
        page: Int,
        size: Int,
        filter: TeamFilterRequest,
        sortParam: SortParam = DEFAULT_SORT,
    ): PageResponse<TeamListItemResponse> {
        val filteredTeams = teamService.findAllFiltered(filter)
        val totalElements = filteredTeams.size.toLong()

        val sortedTeams = filteredTeams.applySorting(sortParam, SORT_FIELD_EXTRACTORS)

        val content = sortedTeams
            .drop(page * size)
            .take(size)
            .map { TeamListItemResponse.from(it) }

        return PageResponse.of(content, totalElements, page, size)
    }

    fun getById(id: UUID): TeamDetailResponse? {
        val team = teamService.get(id) ?: return null
        val players = playerService.getAllByIds(team.playerIds)
        return TeamDetailResponse.from(team, players)
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
            city = request.city,
        )
        teamService.create(team)

        val players = playerService.getAllByIds(request.playerIds)
        return TeamResponse.from(team, players)
    }

    fun update(id: UUID, request: UpdateTeamRequest): TeamResponse? {
        val existingTeam = teamService.get(id) ?: return null

        // Обновляем teamId у игроков, если playerIds был передан
        val oldPlayerIds = existingTeam.playerIds
        val newPlayerIds = request.playerIds ?: existingTeam.playerIds

        // Убираем teamId у игроков, которые были удалены из команды
        (oldPlayerIds - newPlayerIds).forEach { playerId ->
            playerService.get(playerId)?.let { player ->
                playerService.update(player.copy(teamId = null))
            }
        }

        // Добавляем teamId у новых игроков
        (newPlayerIds - oldPlayerIds).forEach { playerId ->
            playerService.get(playerId)?.let { player ->
                playerService.update(player.copy(teamId = id))
            }
        }

        val updatedTeam = existingTeam.copy(
            name = request.name ?: existingTeam.name,
            playerIds = newPlayerIds,
            city = request.city ?: existingTeam.city,
        )
        teamService.delete(id)
        teamService.create(updatedTeam)

        val players = playerService.getAllByIds(newPlayerIds)
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

    fun uploadPhoto(teamId: UUID, file: MultipartFile): PhotoUrlResponse? {
        val team = teamService.get(teamId) ?: return null
        val url = localFileStorageService.upload(file) ?: return null

        val newTeam = team.copy(photoUrl = url)
        teamService.delete(team.id)
        teamService.create(newTeam)
        return PhotoUrlResponse(url)
    }

    fun getPhotoUrl(teamId: UUID): PhotoUrlResponse? {
        val url = teamService.get(teamId)?.photoUrl ?: return null
        return teamService.get(teamId)?.let { PhotoUrlResponse(url) }
    }

    fun deletePhotoUrl(teamId: UUID): PhotoUrlResponse? {
        val team = teamService.get(teamId) ?: return null
        val url = team.photoUrl ?: return PhotoUrlResponse(null)
        val newTeam = team.copy(photoUrl = null)
        teamService.delete(team.id)
        teamService.create(newTeam)
        return PhotoUrlResponse(url)
    }
}
