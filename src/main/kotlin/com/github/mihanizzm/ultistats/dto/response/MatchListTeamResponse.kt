package com.github.mihanizzm.ultistats.dto.response

import java.util.UUID

data class MatchListTeamResponse(
    val teamId: UUID,
    val teamName: String,
    val teamScore: Int,
    val city: String?,
    val photoUrl: String?,
)
