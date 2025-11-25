package com.github.mihanizzm.ultistats.model.events

class HalftimeEndEvent(
    override val time: Double,
) : Event {
    override val type: EventType = EventType.HALFTIME_END
}