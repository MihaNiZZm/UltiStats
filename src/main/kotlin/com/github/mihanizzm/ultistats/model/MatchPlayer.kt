package com.github.mihanizzm.ultistats.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

data class MatchPlayerId(
    val matchId: UUID = UUID(0, 0),
    val playerId: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "match_players")
@IdClass(MatchPlayerId::class)
data class MatchPlayer(
    @Id
    @Column(name = "match_id")
    val matchId: UUID,

    @Id
    @Column(name = "player_id")
    val playerId: UUID,

    @Column(name = "team_id", nullable = false)
    val teamId: UUID,

    @Column
    val number: Int? = null,
)
