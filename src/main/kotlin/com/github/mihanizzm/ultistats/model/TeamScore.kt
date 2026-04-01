package com.github.mihanizzm.ultistats.model

import java.util.UUID

data class TeamScore(
    val teamId: UUID,
    var score: Int = 0,
)
