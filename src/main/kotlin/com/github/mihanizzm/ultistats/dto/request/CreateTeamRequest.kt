package com.github.mihanizzm.ultistats.dto.request

data class CreateTeamRequest(
    val name: String,
    val players: List<CreatePlayerRequest> = emptyList(),
)
