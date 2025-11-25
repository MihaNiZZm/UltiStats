package com.github.mihanizzm.ultistats.model.events

import java.util.*

data class TimeoutEndEvent(
    override val team: UUID,
    override val time: Double,
) : Event, TeamEvent {
    override val type: EventType = EventType.TIMEOUT_END
}
