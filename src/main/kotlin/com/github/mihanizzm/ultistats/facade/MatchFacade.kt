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
import com.github.mihanizzm.ultistats.service.result.MatchCommandResult
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
            val teams = teamService.getAllInListIncludingDeleted(match.teamIds)
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
                val teams = teamService.getAllInListIncludingDeleted(match.teamIds)
                MatchListItemResponse.from(match, teams.associateBy { it.id })
            }

        return PageResponse.of(content, totalElements, page, size)
    }

    fun getById(id: UUID): MatchResponse? {
        val match = matchService.get(id) ?: return null
        val teams = teamService.getAllInListIncludingDeleted(match.teamIds)
        return MatchResponse.from(match, teams.associateBy { it.id })
    }

    fun create(request: CreateMatchRequest): MatchResponse? {
        if (request.teamIds.size != 2 || request.teamIds.distinct().size != 2) return null
        val teams = teamService.getAllInList(request.teamIds)
        if (teams.size != request.teamIds.size) {
            return null
        }
        val match = Match(
            id = UUID.randomUUID(),
            teamIds = request.teamIds,
            plannedStartTimestamp = request.plannedStartTimestamp,
        )
        matchService.create(match)
        return MatchResponse.from(matchService.getOrThrow(match.id), teams.associateBy { it.id })
    }

    fun update(id: UUID, request: UpdateMatchRequest): MatchResponse? {
        val result = matchService.update(id, request.teamIds, request.plannedStartTimestamp)
        return result.toNullableResponse()
    }

    fun delete(id: UUID): Boolean {
        if (matchService.get(id) == null) return false
        matchService.delete(id)
        return true
    }

    fun startMatch(id: UUID, request: MatchTimestampRequest): MatchResponse? {
        return matchService.startMatch(id, request.timestamp).toNullableResponse()
    }

    fun endMatch(id: UUID, request: MatchTimestampRequest): MatchResponse? {
        return matchService.endMatch(id, request.timestamp).toNullableResponse()
    }

    private fun MatchCommandResult<Match>.toNullableResponse(): MatchResponse? = when (this) {
        is MatchCommandResult.Success -> {
            val teams = teamService.getAllInListIncludingDeleted(value.teamIds)
            MatchResponse.from(value, teams.associateBy { it.id })
        }
        MatchCommandResult.NotFound,
        is MatchCommandResult.InvalidRequest,
        is MatchCommandResult.InvalidState,
        is MatchCommandResult.Conflict,
        -> null
    }
}
