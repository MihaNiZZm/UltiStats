package com.github.mihanizzm.ultistats.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "players")
data class Player(
    @Id
    val id: UUID,

    @Transient
    val teamId: UUID?,

    @Transient
    val number: Int?,

    @Column(name = "first_name", nullable = false, length = 255)
    val firstName: String,

    @Column(name = "last_name", nullable = false, length = 255)
    val lastName: String,

    @Column(name = "photo_url", length = 1024)
    val photoUrl: String? = null,

    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
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
