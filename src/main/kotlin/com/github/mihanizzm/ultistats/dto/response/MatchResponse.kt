package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.MatchStatus
import com.github.mihanizzm.ultistats.model.Team
import java.time.Instant
import java.util.UUID

data class MatchResponse(
    val id: UUID,
    val teams: List<MatchTeamResponse>,
    val eventCount: Int,
    val diskHolderId: UUID?,
    val plannedStartTimestamp: Instant?,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val status: MatchStatus,
) {
    companion object {
        fun from(match: Match, teamsById: Map<UUID, Team>) = MatchResponse(
            id = match.id,
            teams = match.teamIds.mapNotNull { teamId ->
                teamsById[teamId]?.let { team ->
                    val teamScore = match.teamScores.find { it.teamId == teamId }?.score ?: 0
                    MatchTeamResponse(
                        teamId = team.id,
                        teamName = team.name,
                        teamScore = teamScore,
                        playerIds = team.playerIds,
                        city = team.city,
                        photoUrl = team.photoUrl,
                    )
                }
            },
            eventCount = match.events.size,
            diskHolderId = match.diskHolderId,
            plannedStartTimestamp = match.plannedStartTimestamp,
            startedAt = match.startedAt,
            endedAt = match.endedAt,
            status = match.status,
        )
    }
}
