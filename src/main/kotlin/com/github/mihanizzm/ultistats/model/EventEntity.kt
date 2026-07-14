package com.github.mihanizzm.ultistats.model

import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.SystemEvent
import com.github.mihanizzm.ultistats.model.events.TeamEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "events")
/**
 * Normalized persistence shape of an event. The public [Event] hierarchy deliberately
 * stays separate so database-only identity, ordering, and soft-deletion fields do not
 * become part of the event API.
 */
data class EventEntity(
    @Id
    val id: UUID,

    @Column(name = "match_id", nullable = false)
    val matchId: UUID,

    @Column(name = "sequence_number", nullable = false)
    val sequenceNumber: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    val eventType: EventType,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant,

    @Column(name = "from_player_id")
    val fromPlayerId: UUID? = null,

    @Column(name = "to_player_id")
    val toPlayerId: UUID? = null,

    @Column(name = "team_id")
    val teamId: UUID? = null,

    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
) {
    fun toDomain(): Event = when (eventType.category) {
        com.github.mihanizzm.ultistats.model.events.EventCategory.ONE_PLAYER -> {
            OnePlayerEvent(requireNotNull(fromPlayerId), occurredAt, eventType)
        }
        com.github.mihanizzm.ultistats.model.events.EventCategory.TWO_PLAYER -> {
            TwoPlayerEvent(
                requireNotNull(fromPlayerId),
                requireNotNull(toPlayerId),
                occurredAt,
                eventType,
            )
        }
        com.github.mihanizzm.ultistats.model.events.EventCategory.TEAM ->
            TeamEvent(requireNotNull(teamId), occurredAt, eventType)
        com.github.mihanizzm.ultistats.model.events.EventCategory.SYSTEM ->
            SystemEvent(occurredAt, eventType)
    }

    companion object {
        fun fromDomain(id: UUID, matchId: UUID, sequenceNumber: Int, event: Event): EventEntity = when (event) {
            is OnePlayerEvent -> EventEntity(
                id, matchId, sequenceNumber, event.type, event.occurredAt, fromPlayerId = event.player,
            )
            is TwoPlayerEvent -> EventEntity(
                id, matchId, sequenceNumber, event.type, event.occurredAt,
                fromPlayerId = event.fromPlayer, toPlayerId = event.toPlayer,
            )
            is TeamEvent -> EventEntity(
                id, matchId, sequenceNumber, event.type, event.occurredAt, teamId = event.team,
            )
            is SystemEvent -> EventEntity(id, matchId, sequenceNumber, event.type, event.occurredAt)
        }
    }
}
