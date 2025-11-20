package com.github.mihanizzm.ultistats.model.statistics

data class AttackStatistics(
    val passes: Int = 0,
    val catches: Int = 0,
    val assists: Int = 0,
    val goals: Int = 0,
    val saves: Int = 0,
    val saveGoals: Int = 0,
    val dropsAsHandler: Int = 0,
    val dropsAsCatcher: Int = 0,
    val discPossessions: Int = 0,
    val pulls: Int = 0,
    val bricks: Int = 0,
)