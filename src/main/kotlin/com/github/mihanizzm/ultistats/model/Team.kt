package com.github.mihanizzm.ultistats.model

import java.util.UUID

data class Team(
    val id: UUID,
    val name: String,
    val playerIds: List<UUID>,
) {
    fun hasPlayer(playerId: UUID): Boolean = playerIds.contains(playerId)
}
