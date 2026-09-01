package com.github.mihanizzm.ultistats.dto.response.statistics

import com.fasterxml.jackson.annotation.JsonInclude
import com.github.mihanizzm.ultistats.model.MatchParticipantKind
import java.util.UUID

@JsonInclude(JsonInclude.Include.ALWAYS)
data class ParticipantStatisticsResponse(
    val participantId: UUID,
    val kind: MatchParticipantKind,
    val unknownSlot: Int?,
    val firstName: String?,
    val lastName: String?,
    val displayName: String,
    val number: Int?,
    val attack: PlayerAttackStatisticsResponse,
    val defense: DefenseStatisticsResponse,
    val time: PlayerTimeStatisticsResponse,
)
