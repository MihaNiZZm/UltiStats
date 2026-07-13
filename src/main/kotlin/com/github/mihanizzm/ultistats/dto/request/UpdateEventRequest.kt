package com.github.mihanizzm.ultistats.dto.request

import com.github.mihanizzm.ultistats.model.events.EventType
import java.time.Instant
import java.util.UUID

/**
 * DTO для частичного обновления события.
 * Все поля необязательны — обновляются только переданные поля.
 */
data class UpdateEventRequest(
    val type: EventType? = null,
    val timestamp: Instant? = null,
    val teamId: UUID? = null,
    val playerId: UUID? = null,
    val toPlayerId: UUID? = null,
)
