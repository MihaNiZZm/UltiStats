package com.github.mihanizzm.ultistats.model.events

import java.time.Instant

class HalftimeStartEvent(
    override val realTimestamp: Instant,
) : Event {
    override val type: EventType = EventType.HALFTIME_START
}