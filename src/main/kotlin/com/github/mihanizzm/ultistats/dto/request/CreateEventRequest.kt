package com.github.mihanizzm.ultistats.dto.request

import com.github.mihanizzm.ultistats.model.events.EventType
import java.time.Instant
import java.util.UUID

data class CreateEventRequest(
    val type: EventType,
    val timestamp: Instant,
    val teamId: UUID? = null,
    val playerId: UUID? = null,
    val toTeamId: UUID? = null,
    val toPlayerId: UUID? = null,
)
