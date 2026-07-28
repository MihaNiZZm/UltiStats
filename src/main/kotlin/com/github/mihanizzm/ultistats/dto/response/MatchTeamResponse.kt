package com.github.mihanizzm.ultistats.dto.response

import java.util.UUID

data class MatchTeamResponse(
    val teamId: UUID,
    val teamName: String,
    val teamScore: Int,
    val participants: List<MatchParticipantResponse>,
    val city: String? = null,
    val photoUrl: String? = null,
)
