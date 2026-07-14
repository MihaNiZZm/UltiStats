package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.TeamPlayer
import java.util.UUID

data class TeamDetailResponse(
    val id: UUID,
    val name: String,
    val players: List<TeamRosterPlayerResponse>,
    val city: String?,
    val photoUrl: String?,
) {
    companion object {
        fun from(team: Team, memberships: List<TeamPlayer>, playersById: Map<UUID, Player>) = TeamDetailResponse(
            id = team.id,
            name = team.name,
            players = memberships.mapNotNull { membership ->
                playersById[membership.playerId]?.let { TeamRosterPlayerResponse.from(membership, it) }
            },
            city = team.city,
            photoUrl = team.photoUrl,
        )
    }
}
