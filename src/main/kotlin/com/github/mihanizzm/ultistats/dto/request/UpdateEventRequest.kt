package com.github.mihanizzm.ultistats.dto.request

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.mihanizzm.ultistats.model.events.EventType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(OnePlayerEventPatchRequest::class, name = "DROP"),
    JsonSubTypes.Type(OnePlayerEventPatchRequest::class, name = "PULL"),
    JsonSubTypes.Type(OnePlayerEventPatchRequest::class, name = "BRICK"),
    JsonSubTypes.Type(OnePlayerEventPatchRequest::class, name = "TURNOVER"),
    JsonSubTypes.Type(TwoPlayerEventPatchRequest::class, name = "PASS"),
    JsonSubTypes.Type(TwoPlayerEventPatchRequest::class, name = "GOAL"),
    JsonSubTypes.Type(TwoPlayerEventPatchRequest::class, name = "BLOCK_MARKER"),
    JsonSubTypes.Type(TwoPlayerEventPatchRequest::class, name = "BLOCK_FIELD"),
    JsonSubTypes.Type(TwoPlayerEventPatchRequest::class, name = "INTERCEPTION"),
    JsonSubTypes.Type(TwoPlayerEventPatchRequest::class, name = "CALLAHAN"),
    JsonSubTypes.Type(TeamEventPatchRequest::class, name = "TIMEOUT_START"),
    JsonSubTypes.Type(TeamEventPatchRequest::class, name = "TIMEOUT_END"),
    JsonSubTypes.Type(SystemEventPatchRequest::class, name = "HALFTIME_START"),
    JsonSubTypes.Type(SystemEventPatchRequest::class, name = "HALFTIME_END"),
)
@Schema(oneOf = [OnePlayerEventPatchRequest::class, TwoPlayerEventPatchRequest::class, TeamEventPatchRequest::class])
sealed interface UpdateEventRequest { val type: EventType }

data class OnePlayerEventPatchRequest(override val type: EventType, val playerId: UUID) : UpdateEventRequest
data class TwoPlayerEventPatchRequest(
    override val type: EventType,
    val fromPlayerId: UUID? = null,
    val toPlayerId: UUID? = null,
) : UpdateEventRequest
data class TeamEventPatchRequest(override val type: EventType, val teamId: UUID) : UpdateEventRequest
data class SystemEventPatchRequest(override val type: EventType) : UpdateEventRequest
