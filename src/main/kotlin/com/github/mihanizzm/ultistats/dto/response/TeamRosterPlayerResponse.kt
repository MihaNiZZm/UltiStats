package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.TeamPlayer
import java.util.UUID

data class TeamRosterPlayerResponse(
    val teamId: UUID,
    val playerId: UUID,
    val number: Int?,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
) {
    companion object {
        fun from(membership: TeamPlayer, player: Player) = TeamRosterPlayerResponse(
            teamId = membership.teamId,
            playerId = membership.playerId,
            number = membership.number,
            firstName = player.firstName,
            lastName = player.lastName,
            photoUrl = player.photoUrl,
        )
    }
}
