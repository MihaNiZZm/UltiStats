package com.github.mihanizzm.ultistats.dto.response.statistics

data class TeamAttackStatisticsResponse(
    val score: Int,
    val completePasses: Int,
    val allPasses: Int,
    val pulls: Int,
    val bricks: Int,
    val possessions: Int,
)
