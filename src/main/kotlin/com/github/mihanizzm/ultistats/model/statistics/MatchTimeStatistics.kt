package com.github.mihanizzm.ultistats.model.statistics

import java.time.Duration

data class MatchTimeStatistics(
    val totalTime: Duration = Duration.ZERO,
    val timeSpentBetweenPoints: Duration = Duration.ZERO,
    val timeSpentOnTimeouts: Duration = Duration.ZERO,
    val timeSpentOnHalftime: Duration = Duration.ZERO,
    val timeSpentOnViolationDiscussions: Duration = Duration.ZERO,
    val pureGameTime: Duration = Duration.ZERO,
)