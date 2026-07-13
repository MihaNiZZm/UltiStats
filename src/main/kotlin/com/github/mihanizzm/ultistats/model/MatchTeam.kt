package com.github.mihanizzm.ultistats.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

data class MatchTeamId(
    val matchId: UUID = UUID(0, 0),
    val teamId: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "match_teams")
@IdClass(MatchTeamId::class)
data class MatchTeam(
    @Id
    @Column(name = "match_id")
    val matchId: UUID,

    @Id
    @Column(name = "team_id")
    val teamId: UUID,

    @Column(nullable = false)
    val position: Int,

    @Column(nullable = false)
    var score: Int = 0,
)
