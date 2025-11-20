package com.github.mihanizzm.ultistats.model.statistics

import java.util.UUID

data class PlayerStatistics(
    val playerId: UUID,
    val attack: AttackStatistics,
    val defense: DefenseStatistics,
    val system: SystemStatistics,
)