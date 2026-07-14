package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.StoredEvent
import com.github.mihanizzm.ultistats.model.events.SystemEvent
import com.github.mihanizzm.ultistats.model.events.TeamEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(oneOf = [OnePlayerEventResponse::class, TwoPlayerEventResponse::class, TeamEventResponse::class, SystemEventResponse::class])
sealed interface EventResponse {
    val id: UUID
    val sequenceNumber: Int
    val type: EventType
    val occurredAt: Instant

    companion object {
        fun from(stored: StoredEvent): EventResponse = when (val event = stored.event) {
            is OnePlayerEvent -> OnePlayerEventResponse(stored.id, stored.sequenceNumber, event.type, event.occurredAt, event.player)
            is TwoPlayerEvent -> TwoPlayerEventResponse(
                stored.id, stored.sequenceNumber, event.type, event.occurredAt, event.fromPlayer, event.toPlayer,
            )
            is TeamEvent -> TeamEventResponse(stored.id, stored.sequenceNumber, event.type, event.occurredAt, event.team)
            is SystemEvent -> SystemEventResponse(stored.id, stored.sequenceNumber, event.type, event.occurredAt)
        }
    }
}

data class OnePlayerEventResponse(
    override val id: UUID,
    override val sequenceNumber: Int,
    override val type: EventType,
    override val occurredAt: Instant,
    val playerId: UUID,
) : EventResponse

data class TwoPlayerEventResponse(
    override val id: UUID,
    override val sequenceNumber: Int,
    override val type: EventType,
    override val occurredAt: Instant,
    val fromPlayerId: UUID,
    val toPlayerId: UUID,
) : EventResponse

data class TeamEventResponse(
    override val id: UUID,
    override val sequenceNumber: Int,
    override val type: EventType,
    override val occurredAt: Instant,
    val teamId: UUID,
) : EventResponse

data class SystemEventResponse(
    override val id: UUID,
    override val sequenceNumber: Int,
    override val type: EventType,
    override val occurredAt: Instant,
) : EventResponse
