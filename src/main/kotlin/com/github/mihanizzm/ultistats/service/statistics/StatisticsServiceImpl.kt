package com.github.mihanizzm.ultistats.service.statistics

import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import com.github.mihanizzm.ultistats.model.statistics.PlayerStatistics
import com.github.mihanizzm.ultistats.model.statistics.TeamStatistics
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamPlayerService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("unused")
class StatisticsServiceImpl(
    private val matchService: MatchService,
    private val playerService: PlayerService,
    private val teamPlayerService: TeamPlayerService,
    private val statisticsAggregatorList: List<StatisticsAggregator>,
) : StatisticsService {
    override fun emptyStatistics(teamIds: List<UUID>): MatchStatistics {
        val teamStats = teamIds
            .map { TeamStatistics(teamId = it) }

        val playerIds = teamIds.flatMap { teamPlayerService.getByTeamId(it).map { membership -> membership.playerId } }
        val playersStats = playerService.getAllByIds(playerIds.distinct())
            .map { PlayerStatistics(participantId = it.id) }

        return MatchStatistics(
            playerStatistics = playersStats,
            teamStatistics = teamStats,
        )
    }

    override fun recalculateMatchStatistics(matchId: UUID): MatchStatistics {
        val match = matchService.getOrThrow(matchId)
        val teamIds = match.teamIds
        val initialStatistics = MatchStatistics(
            playerStatistics = match.participantsByTeam.values.flatten()
                .map { PlayerStatistics(participantId = it.participantId) },
            teamStatistics = teamIds.map { TeamStatistics(teamId = it) },
        )
        val teamByParticipantId = match.participantsByTeam.flatMap { (teamId, participants) ->
            participants.map { it.participantId to teamId }
        }.toMap()

        return statisticsAggregatorList.fold(initialStatistics) { stats, aggregator ->
            aggregator.aggregate(stats, match.events, teamByParticipantId)
        }
    }
}
