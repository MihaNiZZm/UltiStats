package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import java.util.UUID

data class PlayerDetailResponse(
    val id: UUID,
    val team: TeamListItemResponse?,
    val number: Int?,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
) {
    companion object {
        fun from(player: Player, team: Team?): PlayerDetailResponse =
            PlayerDetailResponse(
                id = player.id,
                team = team?.let { TeamListItemResponse.from(it) },
                number = player.number,
                firstName = player.firstName,
                lastName = player.lastName,
                photoUrl = player.photoUrl,
            )
    }
}
