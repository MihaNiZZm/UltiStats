package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Team
import java.util.UUID

data class TeamResponse(
    val id: UUID,
    val name: String,
    val players: List<PlayerResponse>,
) {
    companion object {
        fun from(team: Team) = TeamResponse(
            id = team.id,
            name = team.name,
            players = team.players.map { PlayerResponse.from(it) },
        )
    }
}
