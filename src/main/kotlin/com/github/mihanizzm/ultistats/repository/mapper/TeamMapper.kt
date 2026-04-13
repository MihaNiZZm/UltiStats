package com.github.mihanizzm.ultistats.repository.mapper

import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.repository.entity.TeamEntity

fun TeamEntity.toDomain(playerIds: List<java.util.UUID>): Team = Team(
    id = id,
    name = name,
    playerIds = playerIds,
    city = city,
    photoUrl = photoUrl,
)

fun Team.toEntity(): TeamEntity = TeamEntity(
    id = id,
    name = name,
    city = city,
    photoUrl = photoUrl,
)
