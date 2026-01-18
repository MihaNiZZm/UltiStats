package com.github.mihanizzm.ultistats.service.statistics

import com.github.mihanizzm.ultistats.model.events.StatAffectingEvent
import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import com.github.mihanizzm.ultistats.model.statistics.PlayerStatistics
import com.github.mihanizzm.ultistats.model.statistics.TeamStatistics
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.TeamService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("unused")
class StatisticsServiceImpl(
    private val matchService: MatchService,
    private val teamService: TeamService,
    private val statisticsAggregatorList: List<StatisticsAggregator>,
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

    override fun recalculateMatchStatistics(matchId: UUID): MatchStatistics {
        val match = matchService.getOrThrow(matchId)
        val teamIds = match.teams.map { it.id }

        val gameStatistics = match.events.fold(emptyStatistics(teamIds)) { stat, event ->
            if (event is StatAffectingEvent) event.apply(stat) else stat
        }

        return statisticsAggregatorList.fold(gameStatistics) { stat, statsAggregator ->
            statsAggregator.aggregate(stat, match.events)
        }
    }
}