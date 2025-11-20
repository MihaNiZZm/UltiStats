package com.github.mihanizzm.ultistats.model.events

import java.util.UUID

class TurnoverEvent(
    override val player: UUID,
    override val teamId: UUID,
    override val time: Double,
    override val type: EventType = EventType.TURNOVER,
) : OnePlayerEvent