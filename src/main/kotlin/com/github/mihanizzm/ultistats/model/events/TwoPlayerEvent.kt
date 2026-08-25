package com.github.mihanizzm.ultistats.model.events

import java.time.Instant
import java.util.UUID

/**
 * Событие с двумя участниками матча.
 * Используется для: PASS, GOAL, BLOCK, BLOCK_MARKER, BLOCK_FIELD, INTERCEPTION, CALLAHAN.
 */
data class TwoPlayerEvent(
    val fromParticipant: UUID,
    val toParticipant: UUID,
    override val occurredAt: Instant,
    override val type: EventType,
) : Event {
    init {
        require(type.category == EventCategory.TWO_PLAYER) {
            "EventType $type не является TWO_PLAYER событием"
        }
        require(fromParticipant != toParticipant) {
            "Участники события должны различаться"
        }
    }
}
