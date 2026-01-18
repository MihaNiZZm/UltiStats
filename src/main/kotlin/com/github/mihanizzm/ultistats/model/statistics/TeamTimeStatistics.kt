package com.github.mihanizzm.ultistats.model.statistics

import java.time.Duration

data class TeamTimeStatistics(
    val totalPossessionTime: Duration = Duration.ZERO,
    val totalTimeBetweenPoints: Duration = Duration.ZERO,
    val totalTimeSpentOnTimeouts: Duration = Duration.ZERO,
)
