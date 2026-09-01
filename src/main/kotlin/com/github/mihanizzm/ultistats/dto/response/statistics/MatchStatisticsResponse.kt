package com.github.mihanizzm.ultistats.dto.response.statistics

import java.util.UUID

data class MatchStatisticsResponse(
    val matchId: UUID,
    val teams: List<TeamStatisticsResponse>,
    val time: MatchTimeStatisticsResponse,
)
