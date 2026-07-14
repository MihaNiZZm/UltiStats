package com.github.mihanizzm.ultistats.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "teams")
data class Team(
    @Id
    val id: UUID,

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(length = 127)
    val city: String? = null,

    @Column(name = "photo_url", length = 1024)
    val photoUrl: String? = null,

    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
)
