package com.github.mihanizzm.ultistats.dto.response.statistics

data class DefenseStatisticsResponse(
    val blocks: Int,
    val blocksAsMarker: Int,
    val blocksAsFieldPlayer: Int,
    val interceptions: Int,
    val callahans: Int,
)
