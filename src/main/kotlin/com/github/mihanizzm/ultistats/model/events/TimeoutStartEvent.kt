package com.github.mihanizzm.ultistats.model.events

import java.util.UUID

data class TimeoutStartEvent(
    override val teamId: UUID,
    override val time: Double,
    override val type: EventType = EventType.TIMEOUT_START,
) : Event
