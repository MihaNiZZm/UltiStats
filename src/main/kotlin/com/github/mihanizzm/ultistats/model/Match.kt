package com.github.mihanizzm.ultistats.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.mihanizzm.ultistats.model.events.Event
import java.time.Instant
import java.util.UUID

data class Match(
    val id: UUID,
    val teamIds: List<UUID>,
    val events: MutableList<Event> = mutableListOf(),
    var diskHolderId: UUID? = null,
    val plannedStartTimestamp: Instant? = null,
    var startedAt: Instant? = null,
    var endedAt: Instant? = null,
) {
    @get:JsonIgnore
    val status: MatchStatus
        get() = when {
            endedAt != null -> MatchStatus.FINISHED
            startedAt != null -> MatchStatus.IN_PROGRESS
            else -> MatchStatus.PLANNED
        }
}
