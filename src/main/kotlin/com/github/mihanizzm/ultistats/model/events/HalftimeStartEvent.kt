package com.github.mihanizzm.ultistats.model.events

class HalftimeStartEvent(
    override val time: Double,
) : Event {
    override val type: EventType = EventType.HALFTIME_START
}