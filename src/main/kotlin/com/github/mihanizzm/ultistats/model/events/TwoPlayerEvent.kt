package com.github.mihanizzm.ultistats.model.events

import java.time.Instant
import java.util.UUID

/**
 * Событие с двумя игроками.
 * Используется для: PASS, GOAL, BLOCK_MARKER, BLOCK_FIELD, INTERCEPTION, CALLAHAN.
 */
data class TwoPlayerEvent(
    val fromPlayer: UUID,
    val toPlayer: UUID,
    val fromTeam: UUID,
    val toTeam: UUID,
    override val realTimestamp: Instant,
    override val type: EventType,
) : Event {
    init {
        require(type.category == EventCategory.TWO_PLAYER) {
            "EventType $type не является TWO_PLAYER событием"
        }
    }
}
