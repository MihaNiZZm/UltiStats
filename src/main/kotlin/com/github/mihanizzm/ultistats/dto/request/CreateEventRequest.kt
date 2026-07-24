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
    JsonSubTypes.Type(OnePlayerEventRequest::class, name = "DROP"),
    JsonSubTypes.Type(OnePlayerEventRequest::class, name = "PULL"),
    JsonSubTypes.Type(OnePlayerEventRequest::class, name = "BRICK"),
    JsonSubTypes.Type(OnePlayerEventRequest::class, name = "TURNOVER"),
    JsonSubTypes.Type(TwoPlayerEventRequest::class, name = "PASS"),
    JsonSubTypes.Type(TwoPlayerEventRequest::class, name = "GOAL"),
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
        DiscriminatorMapping(value = "DROP", schema = OnePlayerEventRequest::class),
        DiscriminatorMapping(value = "PULL", schema = OnePlayerEventRequest::class),
        DiscriminatorMapping(value = "BRICK", schema = OnePlayerEventRequest::class),
        DiscriminatorMapping(value = "TURNOVER", schema = OnePlayerEventRequest::class),
        DiscriminatorMapping(value = "PASS", schema = TwoPlayerEventRequest::class),
        DiscriminatorMapping(value = "GOAL", schema = TwoPlayerEventRequest::class),
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
    val type: EventType
    val occurredAt: Instant
}

data class OnePlayerEventRequest(
    override val type: EventType,
    override val occurredAt: Instant,
    val playerId: UUID,
) : CreateEventRequest

data class TwoPlayerEventRequest(
    override val type: EventType,
    override val occurredAt: Instant,
    val fromPlayerId: UUID,
    val toPlayerId: UUID,
) : CreateEventRequest

data class TeamEventRequest(
    override val type: EventType,
    override val occurredAt: Instant,
    val teamId: UUID,
) : CreateEventRequest

data class SystemEventRequest(
    override val type: EventType,
    override val occurredAt: Instant,
) : CreateEventRequest
