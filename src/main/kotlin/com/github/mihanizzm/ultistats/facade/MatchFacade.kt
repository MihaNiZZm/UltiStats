package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.request.CreateMatchRequest
import com.github.mihanizzm.ultistats.dto.response.MatchResponse
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MatchFacade(
    private val matchService: MatchService,
    private val teamService: TeamService,
    private val playerService: PlayerService,
) {
    fun getAll(): List<MatchResponse> =
        matchService.getAll().map { match ->
            MatchResponse.from(match, getPlayersByTeamId(match))
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
        )
        matchService.create(match)
        return MatchResponse.from(match, getPlayersByTeamId(match))
    }

    fun delete(id: UUID): Boolean {
        if (matchService.get(id) == null) return false
        matchService.delete(id)
        return true
    }

    private fun getPlayersByTeamId(match: Match): Map<UUID, List<com.github.mihanizzm.ultistats.model.Player>> =
        match.teams.associate { team ->
            team.id to playerService.getAllByIds(team.playerIds)
        }
}
