package com.github.mihanizzm.ultistats.service.statistics

import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics

interface StatisticsService {
    fun recalculateMatchStatistics(match: Match): MatchStatistics
}
