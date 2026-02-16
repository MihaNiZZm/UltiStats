package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.request.CreateMatchRequest
import com.github.mihanizzm.ultistats.dto.response.MatchResponse
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.TeamService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MatchFacade(
    private val matchService: MatchService,
    private val teamService: TeamService,
) {
    fun getAll(): List<MatchResponse> =
        matchService.getAll().map { MatchResponse.from(it) }

    fun getById(id: UUID): MatchResponse? {
        val match = matchService.get(id) ?: return null
        return MatchResponse.from(match)
    }

    fun create(request: CreateMatchRequest): MatchResponse? {
        val teams = teamService.getAllInList(request.teamIds)
        if (teams.size != request.teamIds.size) {
            return null
        }
        val match = Match(
            id = UUID.randomUUID(),
            teams = teams,
        )
        matchService.create(match)
        return MatchResponse.from(match)
    }

    fun delete(id: UUID): Boolean {
        if (matchService.get(id) == null) return false
        matchService.delete(id)
        return true
    }
}
