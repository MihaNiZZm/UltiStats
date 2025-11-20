package com.github.mihanizzm.ultistats.model

import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import java.time.Instant
import java.util.UUID

data class Match(
    val id: UUID,
    val teams: List<Team>,
    val events: MutableList<Event> = mutableListOf(),
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val statistics: MatchStatistics,
)