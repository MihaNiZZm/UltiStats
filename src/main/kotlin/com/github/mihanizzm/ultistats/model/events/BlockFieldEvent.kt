package com.github.mihanizzm.ultistats.model.events

import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import com.github.mihanizzm.ultistats.model.statistics.PlayerStatistics
import com.github.mihanizzm.ultistats.model.statistics.TeamStatistics
import java.time.Instant
import java.util.UUID

data class BlockFieldEvent(
    override val fromPlayer: UUID,
    override val toPlayer: UUID,
    override val fromTeam: UUID,
    override val toTeam: UUID,
    override val realTimestamp: Instant,
) : TwoPlayerEvent, StatAffectingEvent {
    override val type: EventType = EventType.BLOCK_FIELD

    override fun apply(stats: MatchStatistics): MatchStatistics = changeStatisticsByValue(stats, 1)

    override fun undo(stats: MatchStatistics): MatchStatistics = changeStatisticsByValue(stats, -1)

    private fun changeStatisticsByValue(stats: MatchStatistics, value: Int): MatchStatistics {
        val oldPlayerStats: List<PlayerStatistics> = stats.playerStatistics
        val oldTeamStats: List<TeamStatistics> = stats.teamStatistics

        val newPlayerStats = oldPlayerStats.asSequence()
            .map { stat ->
                when (stat.playerId) {
                    fromPlayer -> stat.copy(
                        attack = stat.attack.copy(dropsOnField = stat.attack.dropsOnField + value)
                    )
                    toPlayer -> stat.copy(
                        defense = stat.defense.copy(blocksAsFieldPlayer = stat.defense.blocksAsFieldPlayer + value)
                    )
                    else -> stat
                }
            }
            .toList()

        val newTeamStats = oldTeamStats.asSequence()
            .map { stat ->
                when (stat.teamId) {
                    fromTeam -> stat.copy(
                        attack = stat.attack.copy(allPasses = stat.attack.allPasses + value)
                    )
                    toTeam -> stat.copy(
                        defense = stat.defense.copy(blocksAsFieldPlayer = stat.defense.blocksAsFieldPlayer + value)
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
