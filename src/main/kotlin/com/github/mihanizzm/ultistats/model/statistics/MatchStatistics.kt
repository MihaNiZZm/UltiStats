package com.github.mihanizzm.ultistats.model.statistics

data class MatchStatistics(
    val playerStatistics: List<PlayerStatistics>,
    val teamStatistics: List<TeamStatistics>,
)
