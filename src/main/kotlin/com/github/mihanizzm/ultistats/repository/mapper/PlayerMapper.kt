package com.github.mihanizzm.ultistats.repository.mapper

import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.repository.entity.PlayerEntity

fun PlayerEntity.toDomain(): Player = Player(
    id = id,
    teamId = teamId,
    number = number,
    firstName = firstName,
    lastName = lastName,
    photoUrl = photoUrl,
)

fun Player.toEntity(): PlayerEntity = PlayerEntity(
    id = id,
    teamId = teamId,
    number = number,
    firstName = firstName,
    lastName = lastName,
    photoUrl = photoUrl,
)
