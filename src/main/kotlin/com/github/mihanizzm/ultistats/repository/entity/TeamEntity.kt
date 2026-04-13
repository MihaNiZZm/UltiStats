package com.github.mihanizzm.ultistats.repository.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "teams")
class TeamEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    var name: String,

    @Column
    var city: String? = null,

    @Column(name = "photo_url")
    var photoUrl: String? = null,
)
