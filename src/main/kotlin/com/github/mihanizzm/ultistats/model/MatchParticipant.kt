package com.github.mihanizzm.ultistats.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

enum class MatchParticipantKind {
    PLAYER,
    UNKNOWN,
}

data class MatchParticipantId(
    val matchId: UUID = UUID(0, 0),
    val participantId: UUID = UUID(0, 0),
) : Serializable

@Entity
@Table(name = "match_participants")
@IdClass(MatchParticipantId::class)
data class MatchParticipant(
    @Id
    @Column(name = "match_id")
    val matchId: UUID,

    @Id
    @Column(name = "participant_id")
    val participantId: UUID,

    @Column(name = "team_id", nullable = false)
    val teamId: UUID,

    @Column(name = "player_id")
    val playerId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    val kind: MatchParticipantKind,

    @Column(name = "unknown_slot")
    val unknownSlot: Int? = null,

    @Column
    val number: Int? = null,
) {
    companion object {
        fun player(matchId: UUID, teamId: UUID, playerId: UUID, number: Int?) = MatchParticipant(
            matchId = matchId,
            participantId = playerId,
            teamId = teamId,
            playerId = playerId,
            kind = MatchParticipantKind.PLAYER,
            number = number,
        )

        fun unknown(matchId: UUID, teamId: UUID, slot: Int): MatchParticipant {
            require(slot in 1..2) { "Unknown participant slot must be 1 or 2" }
            return MatchParticipant(
                matchId = matchId,
                participantId = UUID.randomUUID(),
                teamId = teamId,
                kind = MatchParticipantKind.UNKNOWN,
                unknownSlot = slot,
            )
        }
    }
}
