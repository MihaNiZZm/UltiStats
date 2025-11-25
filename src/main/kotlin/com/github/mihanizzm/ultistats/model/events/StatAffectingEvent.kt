package com.github.mihanizzm.ultistats.model.events

import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics

sealed interface StatAffectingEvent {
    fun apply(stats: MatchStatistics): MatchStatistics

    fun undo(stats: MatchStatistics): MatchStatistics
}
