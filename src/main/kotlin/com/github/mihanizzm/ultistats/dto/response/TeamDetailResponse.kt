package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import java.util.UUID

data class TeamDetailResponse(
    val id: UUID,
    val name: String,
    val players: List<PlayerListItemResponse>,
    val city: String?,
    val photoUrl: String?,
) {
    companion object {
        fun from(team: Team, players: List<Player>): TeamDetailResponse =
            TeamDetailResponse(
                id = team.id,
                name = team.name,
                players = players.map { PlayerListItemResponse.from(it, team) },
                city = team.city,
                photoUrl = team.photoUrl,
            )
    }
}
