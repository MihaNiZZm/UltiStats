package com.github.mihanizzm.ultistats.dto.request

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.mihanizzm.ultistats.model.events.EventType
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(OnePlayerEventRequest::class, name = "INCOMPLETE_PASS"),
    JsonSubTypes.Type(OnePlayerEventRequest::class, name = "PULL"),
    JsonSubTypes.Type(OnePlayerEventRequest::class, name = "BRICK"),
    JsonSubTypes.Type(OnePlayerEventRequest::class, name = "PICKUP"),
    JsonSubTypes.Type(TwoPlayerEventRequest::class, name = "PASS"),
    JsonSubTypes.Type(TwoPlayerEventRequest::class, name = "GOAL"),
    JsonSubTypes.Type(TwoPlayerEventRequest::class, name = "BLOCK"),
    JsonSubTypes.Type(TwoPlayerEventRequest::class, name = "BLOCK_MARKER"),
    JsonSubTypes.Type(TwoPlayerEventRequest::class, name = "BLOCK_FIELD"),
    JsonSubTypes.Type(TwoPlayerEventRequest::class, name = "INTERCEPTION"),
    JsonSubTypes.Type(TwoPlayerEventRequest::class, name = "CALLAHAN"),
    JsonSubTypes.Type(TeamEventRequest::class, name = "TIMEOUT_START"),
    JsonSubTypes.Type(TeamEventRequest::class, name = "TIMEOUT_END"),
    JsonSubTypes.Type(SystemEventRequest::class, name = "HALFTIME_START"),
    JsonSubTypes.Type(SystemEventRequest::class, name = "HALFTIME_END"),
)
@Schema(
    oneOf = [OnePlayerEventRequest::class, TwoPlayerEventRequest::class, TeamEventRequest::class, SystemEventRequest::class],
    discriminatorProperty = "type",
    discriminatorMapping = [
        DiscriminatorMapping(value = "INCOMPLETE_PASS", schema = OnePlayerEventRequest::class),
        DiscriminatorMapping(value = "PULL", schema = OnePlayerEventRequest::class),
        DiscriminatorMapping(value = "BRICK", schema = OnePlayerEventRequest::class),
        DiscriminatorMapping(value = "PICKUP", schema = OnePlayerEventRequest::class),
        DiscriminatorMapping(value = "PASS", schema = TwoPlayerEventRequest::class),
        DiscriminatorMapping(value = "GOAL", schema = TwoPlayerEventRequest::class),
        DiscriminatorMapping(value = "BLOCK", schema = TwoPlayerEventRequest::class),
        DiscriminatorMapping(value = "BLOCK_MARKER", schema = TwoPlayerEventRequest::class),
        DiscriminatorMapping(value = "BLOCK_FIELD", schema = TwoPlayerEventRequest::class),
        DiscriminatorMapping(value = "INTERCEPTION", schema = TwoPlayerEventRequest::class),
        DiscriminatorMapping(value = "CALLAHAN", schema = TwoPlayerEventRequest::class),
        DiscriminatorMapping(value = "TIMEOUT_START", schema = TeamEventRequest::class),
        DiscriminatorMapping(value = "TIMEOUT_END", schema = TeamEventRequest::class),
        DiscriminatorMapping(value = "HALFTIME_START", schema = SystemEventRequest::class),
        DiscriminatorMapping(value = "HALFTIME_END", schema = SystemEventRequest::class),
    ],
)
sealed interface CreateEventRequest {
    @get:Schema(
        description = "Тип события. Он определяет форму запроса и является обязательным.",
        example = "PASS",
    )
    val type: EventType

    @get:Schema(
        description = "Фактическое время события в формате ISO-8601 UTC.",
        example = "2026-07-28T12:30:00Z",
    )
    val occurredAt: Instant
}

@Schema(description = "Событие с одним участником матча: INCOMPLETE_PASS, PULL, BRICK или PICKUP.")
data class OnePlayerEventRequest(
    override val type: EventType,
    override val occurredAt: Instant,
    val participantId: UUID,
) : CreateEventRequest

@Schema(description = "Событие с двумя участниками матча: PASS, GOAL, BLOCK, BLOCK_MARKER, BLOCK_FIELD, INTERCEPTION или CALLAHAN.")
data class TwoPlayerEventRequest(
    override val type: EventType,
    override val occurredAt: Instant,
    val fromParticipantId: UUID,
    val toParticipantId: UUID,
) : CreateEventRequest

@Schema(description = "Командное событие: TIMEOUT_START или TIMEOUT_END.")
data class TeamEventRequest(
    override val type: EventType,
    override val occurredAt: Instant,
    val teamId: UUID,
) : CreateEventRequest

@Schema(description = "Системное событие: HALFTIME_START или HALFTIME_END.")
data class SystemEventRequest(
    override val type: EventType,
    override val occurredAt: Instant,
) : CreateEventRequest
