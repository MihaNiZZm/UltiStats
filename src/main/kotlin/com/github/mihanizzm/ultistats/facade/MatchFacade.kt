package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.common.PageResponse
import com.github.mihanizzm.ultistats.dto.common.SortParam
import com.github.mihanizzm.ultistats.dto.request.CreateMatchRequest
import com.github.mihanizzm.ultistats.dto.request.MatchFilterRequest
import com.github.mihanizzm.ultistats.dto.request.MatchTimestampRequest
import com.github.mihanizzm.ultistats.dto.request.UpdateMatchRequest
import com.github.mihanizzm.ultistats.dto.response.MatchResponse
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import com.github.mihanizzm.ultistats.util.SortingUtils.applySorting
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MatchFacade(
    private val matchService: MatchService,
    private val teamService: TeamService,
    private val playerService: PlayerService,
) {
    companion object {
        val DEFAULT_SORT = SortParam("plannedStartTimestamp")

        val SORT_FIELD_EXTRACTORS: Map<String, (Match) -> Comparable<*>?> = mapOf(
            "plannedStartTimestamp" to { it.plannedStartTimestamp },
            "startedAt" to { it.startedAt },
            "endedAt" to { it.endedAt },
            "status" to { it.status.name },
        )
    }

    fun getAll(): List<MatchResponse> =
        matchService.getAll().map { match ->
            MatchResponse.from(match, getPlayersByTeamId(match))
        }

    fun getAllPaged(
        page: Int,
        size: Int,
        filter: MatchFilterRequest,
        sortParam: SortParam = DEFAULT_SORT,
    ): PageResponse<MatchResponse> {
        val filteredMatches = matchService.findAllFiltered(filter)
        val totalElements = filteredMatches.size.toLong()

        val sortedMatches = filteredMatches.applySorting(sortParam, SORT_FIELD_EXTRACTORS)

        val content = sortedMatches
            .drop(page * size)
            .take(size)
            .map { match ->
                MatchResponse.from(match, getPlayersByTeamId(match))
            }

        return PageResponse.of(content, totalElements, page, size)
    }

    fun getById(id: UUID): MatchResponse? {
        val match = matchService.get(id) ?: return null
        return MatchResponse.from(match, getPlayersByTeamId(match))
    }

    fun create(request: CreateMatchRequest): MatchResponse? {
        val teams = teamService.getAllInList(request.teamIds)
        if (teams.size != request.teamIds.size) {
            return null
        }
        val match = Match(
            id = UUID.randomUUID(),
            teams = teams,
            plannedStartTimestamp = request.plannedStartTimestamp,
        )
        matchService.create(match)
        return MatchResponse.from(match, getPlayersByTeamId(match))
    }

    fun update(id: UUID, request: UpdateMatchRequest): MatchResponse? {
        val existingMatch = matchService.get(id) ?: return null

        val newTeamIds = request.teamIds ?: existingMatch.teams.map { it.id }
        val newTeams = teamService.getAllInList(newTeamIds)
        if (newTeams.size != newTeamIds.size) {
            return null
        }

        val updatedMatch = existingMatch.copy(
            teams = newTeams,
            plannedStartTimestamp = request.plannedStartTimestamp ?: existingMatch.plannedStartTimestamp,
        )
        matchService.delete(id)
        matchService.create(updatedMatch)
        return MatchResponse.from(updatedMatch, getPlayersByTeamId(updatedMatch))
    }

    fun delete(id: UUID): Boolean {
        if (matchService.get(id) == null) return false
        matchService.delete(id)
        return true
    }

    fun startMatch(id: UUID, request: MatchTimestampRequest): MatchResponse? {
        val match = matchService.get(id) ?: return null
        if (!matchService.startMatch(id, request.timestamp)) {
            return null
        }
        return MatchResponse.from(match, getPlayersByTeamId(match))
    }

    fun endMatch(id: UUID, request: MatchTimestampRequest): MatchResponse? {
        val match = matchService.get(id) ?: return null
        if (!matchService.endMatch(id, request.timestamp)) {
            return null
        }
        return MatchResponse.from(match, getPlayersByTeamId(match))
    }

    private fun getPlayersByTeamId(match: Match): Map<UUID, List<Player>> =
        match.teams.associate { team ->
            team.id to playerService.getAllByIds(team.playerIds)
        }
}
