package com.github.mihanizzm.ultistats.model.events

import java.util.UUID

data class PassEvent(
    override val fromPlayer: UUID,
    override val toPlayer: UUID,
    override val teamId: UUID,
    override val time: Double,
    override val type: EventType = EventType.PASS,
    val isSave: Boolean = false,
    val isGoal: Boolean = false,
) : TwoPlayerEvent