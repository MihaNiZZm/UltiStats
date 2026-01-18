package com.github.mihanizzm.ultistats.model.events

import java.time.Instant
import java.util.*

data class TimeoutEndEvent(
    override val team: UUID,
    override val realTimestamp: Instant,
) : Event, TeamEvent {
    override val type: EventType = EventType.TIMEOUT_END
}
