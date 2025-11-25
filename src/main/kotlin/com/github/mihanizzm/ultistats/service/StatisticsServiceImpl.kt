package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.events.StatAffectingEvent
import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import com.github.mihanizzm.ultistats.model.statistics.PlayerStatistics
import com.github.mihanizzm.ultistats.model.statistics.TeamStatistics
import org.springframework.stereotype.Service
import java.util.*

@Service
@Suppress("unused")
class StatisticsServiceImpl(
    private val matchService: MatchService,
    private val teamService: TeamService,
) : StatisticsService {
    override fun emptyStatistics(teamIds: List<UUID>): MatchStatistics {
        val teamStats = teamIds
            .map { TeamStatistics(teamId = it) }

        val playersStats = teamService.getAllInList(teamIds)
            .flatMap { it.players }
            .mapNotNull { it.id }
            .map { PlayerStatistics(playerId = it) }

        return MatchStatistics(
            playerStatistics = playersStats,
            teamStatistics = teamStats,
        )
    }

    override fun calculateStatistics(matchId: UUID): MatchStatistics {
        val match = matchService.get(matchId)
        val teamIds = match.teams.map { it.id }

        return match.events.fold(emptyStatistics(teamIds)) { stat, event ->
            if (event is StatAffectingEvent) event.apply(stat) else stat
        }
    }
}