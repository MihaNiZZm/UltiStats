package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import java.util.*

interface StatisticsService {
    fun emptyStatistics(teamIds: List<UUID>): MatchStatistics

    fun calculateStatistics(matchId: UUID): MatchStatistics
}
