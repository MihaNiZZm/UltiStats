package com.github.mihanizzm.ultistats.dto.response.statistics

data class TeamTimeStatisticsResponse(
    val possessionTimeMs: Long,
    val betweenPointsTimeMs: Long,
    val timeoutTimeMs: Long,
)
