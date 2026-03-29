package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import java.util.UUID

data class TeamResponse(
    val id: UUID,
    val name: String,
    val players: List<PlayerResponse>,
    val city: String? = null,
    val photoUrl: String? = null,
) {
    companion object {
        fun from(team: Team, players: List<Player>) = TeamResponse(
            id = team.id,
            name = team.name,
            players = players.map { PlayerResponse.from(it) },
            city = team.city,
            photoUrl = team.photoUrl,
        )
    }
}
