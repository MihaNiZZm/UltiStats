package com.github.mihanizzm.ultistats.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

data class TeamPlayerId(
    val teamId: UUID = UUID(0, 0),
    val playerId: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "team_players")
@IdClass(TeamPlayerId::class)
data class TeamPlayer(
    @Id
    @Column(name = "team_id")
    val teamId: UUID,

    @Id
    @Column(name = "player_id")
    val playerId: UUID,

    @Column
    val number: Int? = null,

    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
)
