package com.github.mihanizzm.ultistats.model.events

import java.time.Instant

class HalftimeEndEvent(
    override val realTimestamp: Instant,
) : Event {
    override val type: EventType = EventType.HALFTIME_END
}