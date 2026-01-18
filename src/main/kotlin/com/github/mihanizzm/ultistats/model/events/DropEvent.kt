package com.github.mihanizzm.ultistats.model.events

import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import com.github.mihanizzm.ultistats.model.statistics.PlayerStatistics
import com.github.mihanizzm.ultistats.model.statistics.TeamStatistics
import java.time.Instant
import java.util.UUID

data class DropEvent(
    override val player: UUID,
    override val team: UUID,
    override val realTimestamp: Instant,
) : OnePlayerEvent, StatAffectingEvent {
    override val type: EventType = EventType.DROP

    override fun apply(stats: MatchStatistics): MatchStatistics = changeStatisticsByValue(stats, 1)

    override fun undo(stats: MatchStatistics): MatchStatistics = changeStatisticsByValue(stats, -1)

    private fun changeStatisticsByValue(stats: MatchStatistics, value: Int): MatchStatistics {
        val oldPlayerStats: List<PlayerStatistics> = stats.playerStatistics
        val oldTeamStats: List<TeamStatistics> = stats.teamStatistics

        val newPlayerStats = oldPlayerStats.asSequence()
            .map { stat ->
                when (stat.playerId) {
                    player -> stat.copy(
                        attack = stat.attack.copy(drops = stat.attack.drops + value)
                    )
                    else -> stat
                }
            }
            .toList()

        val newTeamStats = oldTeamStats.asSequence()
            .map { stat ->
                when (stat.teamId) {
                    team -> stat.copy(
                        attack = stat.attack.copy(allPasses = stat.attack.allPasses + value)
                    )
                    else -> stat
                }
            }
            .toList()

        return stats.copy(
            playerStatistics = newPlayerStats,
            teamStatistics = newTeamStats,
        )
    }
}
