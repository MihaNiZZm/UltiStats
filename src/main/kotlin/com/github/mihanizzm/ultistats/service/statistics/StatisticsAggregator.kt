package com.github.mihanizzm.ultistats.service.statistics

import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics

interface StatisticsAggregator {
    fun aggregate(previousStatisticsState: MatchStatistics, events: List<Event>): MatchStatistics
}