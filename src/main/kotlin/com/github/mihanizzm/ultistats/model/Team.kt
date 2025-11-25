package com.github.mihanizzm.ultistats.model

import java.util.UUID

data class Team(
    val id: UUID,
    val name: String,
    val players: List<Player>,
) {
    fun hasPlayer(playerId: UUID): Boolean = players.map { it.id }.contains(playerId)
}
