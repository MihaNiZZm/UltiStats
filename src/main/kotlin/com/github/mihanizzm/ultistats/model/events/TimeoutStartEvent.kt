package com.github.mihanizzm.ultistats.model.events

import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import java.util.UUID

data class TimeoutStartEvent(
    override val team: UUID,
    override val time: Double,
) : Event, TeamEvent {
    override val type: EventType = EventType.TIMEOUT_START
}
