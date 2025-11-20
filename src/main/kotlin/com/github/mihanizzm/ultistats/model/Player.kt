package com.github.mihanizzm.ultistats.model

import java.util.UUID

data class Player(
    val id: UUID?,
    val number: Int?,
    val firstName: String,
    val lastName: String,
    val teamId: UUID,
) {
    companion object {
        fun unknown(teamId: UUID) = Player (
            id = null,
            number = null,
            firstName = "N/A",
            lastName = "N/A",
            teamId = teamId,
        )
    }
}