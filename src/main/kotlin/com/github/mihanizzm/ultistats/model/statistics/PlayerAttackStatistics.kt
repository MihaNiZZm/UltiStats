package com.github.mihanizzm.ultistats.model.statistics

data class PlayerAttackStatistics(
    val passes: Int = 0,
    val catches: Int = 0,
    val assists: Int = 0,
    val goals: Int = 0,
    val saves: Int = 0,
    val saveGoals: Int = 0,
    val dropsOnMarker: Int = 0,
    val dropsOnField: Int = 0,
    val drops: Int = 0,
    val callahanDrops: Int = 0,
    val discPossessions: Int = 0,
    val pulls: Int = 0,
    val bricks: Int = 0,
)
