package com.github.mihanizzm.ultistats.dto.request

import java.util.UUID

data class CreateTeamRequest(
    val name: String,
    val playerIds: List<UUID> = emptyList(),
)
