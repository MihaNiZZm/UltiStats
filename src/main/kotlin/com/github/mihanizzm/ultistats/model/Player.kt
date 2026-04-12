package com.github.mihanizzm.ultistats.model

import java.util.UUID

data class Player(
    val id: UUID,
    val teamId: UUID?,
    val number: Int?,
    val firstName: String,
    val lastName: String,
    val photoUrl: String? = null,
) {
    companion object {
        fun unknown(teamId: UUID) = Player(
            id = UUID.randomUUID(),
            teamId = teamId,
            number = null,
            firstName = "N/A",
            lastName = "N/A",
        )
    }
}
