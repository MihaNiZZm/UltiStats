package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.TeamPlayer
import java.util.UUID

data class TeamPlayerResponse(
    val teamId: UUID,
    val playerId: UUID,
    val number: Int?,
) {
    companion object {
        fun from(membership: TeamPlayer) = TeamPlayerResponse(
            teamId = membership.teamId,
            playerId = membership.playerId,
            number = membership.number,
        )
    }
}
