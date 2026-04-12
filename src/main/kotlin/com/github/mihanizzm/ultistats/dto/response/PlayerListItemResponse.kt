package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import java.util.UUID

data class PlayerListItemResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val teamName: String?,
    val number: Int?,
    val photoUrl: String?,
) {
    companion object {
        fun from(player: Player, team: Team?): PlayerListItemResponse =
            PlayerListItemResponse(
                id = player.id,
                firstName = player.firstName,
                lastName = player.lastName,
                teamName = team?.name,
                number = player.number,
                photoUrl = player.photoUrl,
            )
    }
}
