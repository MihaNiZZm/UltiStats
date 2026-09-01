package com.github.mihanizzm.ultistats.dto.response.statistics

import java.util.UUID

data class TeamStatisticsResponse(
    val teamId: UUID,
    val teamName: String,
    val attack: TeamAttackStatisticsResponse,
    val defense: DefenseStatisticsResponse,
    val time: TeamTimeStatisticsResponse,
    val participants: List<ParticipantStatisticsResponse>,
)
