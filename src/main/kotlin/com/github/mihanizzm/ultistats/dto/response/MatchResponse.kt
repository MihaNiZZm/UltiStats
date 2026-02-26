package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import java.time.Instant
import java.util.UUID

data class MatchResponse(
    val id: UUID,
    val teams: List<TeamResponse>,
    val eventCount: Int,
    val diskHolderId: UUID?,
    val plannedStartTimestamp: Instant?,
    val startedAt: Instant?,
    val endedAt: Instant?,
) {
    companion object {
        fun from(match: Match, playersByTeamId: Map<UUID, List<Player>>) = MatchResponse(
            id = match.id,
            teams = match.teams.map { team ->
                TeamResponse.from(team, playersByTeamId[team.id] ?: emptyList())
            },
            eventCount = match.events.size,
            diskHolderId = match.diskHolderId,
            plannedStartTimestamp = match.plannedStartTimestamp,
            startedAt = match.startedAt,
            endedAt = match.endedAt,
        )
    }
}
