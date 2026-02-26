package com.github.mihanizzm.ultistats.model.events

import java.time.Instant
import java.util.UUID

/**
 * Событие с одним игроком.
 * Используется для: PULL, BRICK, DROP, TURNOVER.
 */
data class OnePlayerEvent(
    val player: UUID,
    val team: UUID,
    override val realTimestamp: Instant,
    override val type: EventType,
) : Event {
    init {
        require(type.category == EventCategory.ONE_PLAYER) {
            "EventType $type не является ONE_PLAYER событием"
        }
    }
}
