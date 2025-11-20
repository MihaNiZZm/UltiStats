package com.github.mihanizzm.ultistats.model.events

import java.util.UUID

data class PullEvent(
    override val player: UUID,
    override val teamId: UUID,
    override val time: Double,
    override val type: EventType = EventType.PULL,
) : OnePlayerEvent