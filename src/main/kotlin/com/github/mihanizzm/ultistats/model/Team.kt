package com.github.mihanizzm.ultistats.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.util.UUID

@Entity
@Table(name = "teams")
data class Team(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val name: String,

    @Transient
    val playerIds: List<UUID> = emptyList(),

    @Column
    val city: String? = null,

    @Column(name = "photo_url")
    val photoUrl: String? = null,
) {
    fun hasPlayer(playerId: UUID): Boolean = playerIds.contains(playerId)
}
