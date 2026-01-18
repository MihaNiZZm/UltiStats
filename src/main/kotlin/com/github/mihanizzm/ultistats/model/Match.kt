package com.github.mihanizzm.ultistats.model

import com.github.mihanizzm.ultistats.model.events.Event
import java.util.UUID

data class Match(
    val id: UUID,
    val teams: List<Team>,
    val events: MutableList<Event> = mutableListOf(),
)
