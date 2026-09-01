package com.github.mihanizzm.ultistats.dto.response.statistics

data class PlayerAttackStatisticsResponse(
    val passes: Int,
    val catches: Int,
    val assists: Int,
    val goals: Int,
    val dropsOnMarker: Int,
    val dropsOnField: Int,
    val incompletePasses: Int,
    val callahanDrops: Int,
    val discPossessions: Int,
    val pulls: Int,
    val bricks: Int,
)
