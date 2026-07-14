package com.github.mihanizzm.ultistats.dto.request

data class CreateTeamRequest(
    val name: String,
    val city: String? = null,
)
