package com.github.mihanizzm.ultistats.model.events

import java.util.UUID

data class TimeoutEndEvent(
    override val teamId: UUID,
    override val time: Double,
    override val type: EventType = EventType.TIMEOUT_END,
) : Event