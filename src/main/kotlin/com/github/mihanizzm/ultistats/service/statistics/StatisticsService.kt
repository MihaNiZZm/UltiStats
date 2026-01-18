package com.github.mihanizzm.ultistats.service.statistics

import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import java.util.UUID

interface StatisticsService {
    fun emptyStatistics(teamIds: List<UUID>): MatchStatistics

    fun recalculateMatchStatistics(matchId: UUID): MatchStatistics
}
