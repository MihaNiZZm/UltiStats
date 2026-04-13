package com.github.mihanizzm.ultistats.repository.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "players")
class PlayerEntity(
    @Id
    val id: UUID,

    @Column(name = "team_id")
    var teamId: UUID?,

    @Column
    var number: Int? = null,

    @Column(name = "first_name", nullable = false)
    var firstName: String,

    @Column(name = "last_name", nullable = false)
    var lastName: String,

    @Column(name = "photo_url")
    var photoUrl: String? = null,
)
