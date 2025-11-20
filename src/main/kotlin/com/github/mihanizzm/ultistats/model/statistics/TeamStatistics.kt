package com.github.mihanizzm.ultistats.model.statistics

import java.util.UUID

data class TeamStatistics(
    val teamId: UUID,
    val attack: TeamAttackStatistics,
    val defense: DefenseStatistics,
    val system: SystemStatistics,
)