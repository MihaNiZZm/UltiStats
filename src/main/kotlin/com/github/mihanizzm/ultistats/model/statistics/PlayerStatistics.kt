package com.github.mihanizzm.ultistats.model.statistics

import java.util.UUID

data class PlayerStatistics(
    val playerId: UUID,
    val attack: AttackStatistics = AttackStatistics(),
    val defense: DefenseStatistics = DefenseStatistics(),
    val system: SystemStatistics = SystemStatistics(),
)
