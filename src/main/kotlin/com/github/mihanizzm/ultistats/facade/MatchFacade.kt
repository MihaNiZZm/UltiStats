package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.common.PageResponse
import com.github.mihanizzm.ultistats.dto.common.SortParam
import com.github.mihanizzm.ultistats.dto.request.CreateMatchRequest
import com.github.mihanizzm.ultistats.dto.request.MatchFilterRequest
import com.github.mihanizzm.ultistats.dto.request.MatchTimestampRequest
import com.github.mihanizzm.ultistats.dto.request.UpdateMatchRequest
import com.github.mihanizzm.ultistats.dto.response.MatchListItemResponse
import com.github.mihanizzm.ultistats.dto.response.MatchResponse
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.TeamService
import com.github.mihanizzm.ultistats.util.SortingUtils.applySorting
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MatchFacade(
    private val matchService: MatchService,
    private val teamService: TeamService,
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
            val teams = teamService.getAllInList(match.teamIds)
            MatchResponse.from(match, teams.associateBy { it.id })
        }

    fun getAllPaged(
        page: Int,
        size: Int,
        filter: MatchFilterRequest,
        sortParam: SortParam = DEFAULT_SORT,
    ): PageResponse<MatchListItemResponse> {
        val filteredMatches = matchService.findAllFiltered(filter)
        val totalElements = filteredMatches.size.toLong()

        val sortedMatches = filteredMatches.applySorting(sortParam, SORT_FIELD_EXTRACTORS)

        val content = sortedMatches
            .drop(page * size)
            .take(size)
            .map { match ->
                val teams = teamService.getAllInList(match.teamIds)
                MatchListItemResponse.from(match, teams.associateBy { it.id })
            }

        return PageResponse.of(content, totalElements, page, size)
    }

    fun getById(id: UUID): MatchResponse? {
        val match = matchService.get(id) ?: return null
        val teams = teamService.getAllInList(match.teamIds)
        return MatchResponse.from(match, teams.associateBy { it.id })
    }

    fun create(request: CreateMatchRequest): MatchResponse? {
        val teams = teamService.getAllInList(request.teamIds)
        if (teams.size != request.teamIds.size) {
            return null
        }
        val match = Match(
            id = UUID.randomUUID(),
            teamIds = request.teamIds,
            plannedStartTimestamp = request.plannedStartTimestamp,
        )
        match.initTeamScores()
        matchService.create(match)
        return MatchResponse.from(match, teams.associateBy { it.id })
    }

    fun update(id: UUID, request: UpdateMatchRequest): MatchResponse? {
        val existingMatch = matchService.get(id) ?: return null

        val newTeamIds = request.teamIds ?: existingMatch.teamIds
        val teams = teamService.getAllInList(newTeamIds)
        val teamsById = teams.associateBy { it.id }
        if (teams.size != newTeamIds.size) {
            return null
        }

        val updatedMatch = existingMatch.copy(
            teamIds = newTeamIds,
            plannedStartTimestamp = request.plannedStartTimestamp ?: existingMatch.plannedStartTimestamp,
        )
        matchService.update(updatedMatch)
        return MatchResponse.from(updatedMatch, teamsById)
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
        val teams = teamService.getAllInList(match.teamIds)
        return MatchResponse.from(match, teams.associateBy { it.id })
    }

    fun endMatch(id: UUID, request: MatchTimestampRequest): MatchResponse? {
        val match = matchService.get(id) ?: return null
        if (!matchService.endMatch(id, request.timestamp)) {
            return null
        }
        val teams = teamService.getAllInList(match.teamIds)
        return MatchResponse.from(match, teams.associateBy { it.id })
    }
}
