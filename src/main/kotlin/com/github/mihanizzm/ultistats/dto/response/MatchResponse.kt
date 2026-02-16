package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Match
import java.util.UUID

data class MatchResponse(
    val id: UUID,
    val teams: List<TeamResponse>,
    val eventCount: Int,
    val diskHolderId: UUID?,
) {
    companion object {
        fun from(match: Match) = MatchResponse(
            id = match.id,
            teams = match.teams.map { TeamResponse.from(it) },
            eventCount = match.events.size,
            diskHolderId = match.diskHolderId,
        )
    }
}
