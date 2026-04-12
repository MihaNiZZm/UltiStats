package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.common.PageResponse
import com.github.mihanizzm.ultistats.dto.common.SortParam
import com.github.mihanizzm.ultistats.dto.request.CreatePlayerRequest
import com.github.mihanizzm.ultistats.dto.request.PlayerFilterRequest
import com.github.mihanizzm.ultistats.dto.request.UpdatePlayerRequest
import com.github.mihanizzm.ultistats.dto.response.PhotoUrlResponse
import com.github.mihanizzm.ultistats.dto.response.PlayerListItemResponse
import com.github.mihanizzm.ultistats.dto.response.PlayerResponse
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.service.LocalFileStorageService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import com.github.mihanizzm.ultistats.util.SortingUtils.applySorting
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Component
class PlayerFacade(
    private val playerService: PlayerService,
    private val teamService: TeamService,
    private val localFileStorageService: LocalFileStorageService,
) {
    companion object {
        val DEFAULT_SORT = SortParam("lastName")

        val SORT_FIELD_EXTRACTORS: Map<String, (Player) -> Comparable<*>?> = mapOf(
            "lastName" to { it.lastName },
            "firstName" to { it.firstName },
            "number" to { it.number },
            "teamId" to { it.teamId?.toString() },
        )
    }

    fun getAll(): List<PlayerResponse> =
        playerService.getAll().map { PlayerResponse.from(it) }

    fun getAllPaged(
        page: Int,
        size: Int,
        filter: PlayerFilterRequest,
        sortParam: SortParam = DEFAULT_SORT,
    ): PageResponse<PlayerListItemResponse> {
        val filteredPlayers = playerService.findAllFiltered(filter)
        val totalElements = filteredPlayers.size.toLong()

        val sortedPlayers = filteredPlayers.applySorting(sortParam, SORT_FIELD_EXTRACTORS)

        val content = sortedPlayers
            .drop(page * size)
            .take(size)
            .map { player ->
                val team = player.teamId?.let { teamService.get(it) }
                PlayerListItemResponse.from(player, team)
            }

        return PageResponse.of(content, totalElements, page, size)
    }

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

    fun update(id: UUID, request: UpdatePlayerRequest): PlayerResponse? {
        val existingPlayer = playerService.get(id) ?: return null
        val oldTeamId = existingPlayer.teamId
        // teamId может быть null в request, что означает "не менять"
        // но также teamId может быть null у игрока (игрок без команды)
        val shouldUpdateTeam = request.teamId != null
        val newTeamId = request.teamId

        val updatedPlayer = existingPlayer.copy(
            number = request.number ?: existingPlayer.number,
            firstName = request.firstName ?: existingPlayer.firstName,
            lastName = request.lastName ?: existingPlayer.lastName,
            teamId = if (shouldUpdateTeam) newTeamId else existingPlayer.teamId,
        )
        playerService.update(updatedPlayer)

        // Обновляем связи с командами только если teamId был явно передан
        if (shouldUpdateTeam && oldTeamId != newTeamId) {
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

    fun uploadPhoto(playerId: UUID, file: MultipartFile): PhotoUrlResponse? {
        val player = playerService.get(playerId) ?: return null
        val url = localFileStorageService.upload(file) ?: return null

        val updatedPlayer = player.copy(photoUrl = url)
        playerService.update(updatedPlayer)
        return PhotoUrlResponse(url)
    }

    fun getPhotoUrl(playerId: UUID): PhotoUrlResponse? {
        val url = playerService.get(playerId)?.photoUrl ?: return null
        return PhotoUrlResponse(url)
    }

    fun deletePhotoUrl(playerId: UUID): PhotoUrlResponse? {
        val player = playerService.get(playerId) ?: return null
        val url = player.photoUrl ?: return PhotoUrlResponse(null)
        val updatedPlayer = player.copy(photoUrl = null)
        playerService.update(updatedPlayer)
        return PhotoUrlResponse(url)
    }
}
