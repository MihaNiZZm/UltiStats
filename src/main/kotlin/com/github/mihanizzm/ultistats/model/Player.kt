package com.github.mihanizzm.ultistats.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "players")
data class Player(
    @Id
    val id: UUID,

    @Column(name = "team_id")
    val teamId: UUID?,

    @Column
    val number: Int?,

    @Column(name = "first_name", nullable = false)
    val firstName: String,

    @Column(name = "last_name", nullable = false)
    val lastName: String,

    @Column(name = "photo_url")
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
