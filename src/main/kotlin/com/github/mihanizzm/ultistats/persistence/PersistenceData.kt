package com.github.mihanizzm.ultistats.persistence

import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import java.time.Instant

data class PersistenceData(
    val version: Int = 1,
    val savedAt: Instant = Instant.now(),
    val matches: List<Match> = emptyList(),
    val teams: List<Team> = emptyList(),
    val players: List<Player> = emptyList(),
)
