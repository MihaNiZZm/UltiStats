package com.github.mihanizzm.ultistats.model.statistics

import java.util.UUID

data class TeamStatistics(
    val teamId: UUID,
    val attack: TeamAttackStatistics = TeamAttackStatistics(),
    val defense: DefenseStatistics = DefenseStatistics(),
    val time: TeamTimeStatistics = TeamTimeStatistics(),
)
