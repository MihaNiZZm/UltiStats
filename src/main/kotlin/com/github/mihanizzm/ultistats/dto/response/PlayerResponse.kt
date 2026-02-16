package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Player
import java.util.UUID

data class PlayerResponse(
    val id: UUID?,
    val teamId: UUID,
    val number: Int?,
    val firstName: String,
    val lastName: String,
) {
    companion object {
        fun from(player: Player) = PlayerResponse(
            id = player.id,
            teamId = player.teamId,
            number = player.number,
            firstName = player.firstName,
            lastName = player.lastName,
        )
    }
}
