package com.github.mihanizzm.ultistats.model.statistics

import java.util.UUID

data class PlayerStatistics(
    val participantId: UUID,
    val attack: PlayerAttackStatistics = PlayerAttackStatistics(),
    val defense: DefenseStatistics = DefenseStatistics(),
    val time: PlayerTimeStatistics = PlayerTimeStatistics(),
)
