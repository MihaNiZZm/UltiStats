package com.github.mihanizzm.ultistats.model.events

import java.time.Instant
import java.util.UUID

/**
 * Событие с одним участником матча.
 * Используется для: PULL, BRICK, INCOMPLETE_PASS, PICKUP.
 */
data class OnePlayerEvent(
    val participant: UUID,
    override val occurredAt: Instant,
    override val type: EventType,
) : Event {
    init {
        require(type.category == EventCategory.ONE_PLAYER) {
            "EventType $type не является ONE_PLAYER событием"
        }
    }
}
