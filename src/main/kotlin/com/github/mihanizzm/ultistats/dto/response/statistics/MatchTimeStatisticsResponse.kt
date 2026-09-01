package com.github.mihanizzm.ultistats.dto.response.statistics

data class MatchTimeStatisticsResponse(
    val totalTimeMs: Long,
    val betweenPointsTimeMs: Long,
    val timeoutTimeMs: Long,
    val halftimeTimeMs: Long,
    val pureGameTimeMs: Long,
)
