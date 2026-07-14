package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Player
import java.util.UUID

data class PlayerListItemResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val photoUrl: String?,
) {
    companion object {
        fun from(player: Player) = PlayerListItemResponse(
            id = player.id,
            firstName = player.firstName,
            lastName = player.lastName,
            photoUrl = player.photoUrl,
        )
    }
}
