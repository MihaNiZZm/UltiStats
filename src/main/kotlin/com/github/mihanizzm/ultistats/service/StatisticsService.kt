package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import java.util.UUID

interface StatisticsService {
    fun calculateStatistics(matchId: UUID): MatchStatistics
}