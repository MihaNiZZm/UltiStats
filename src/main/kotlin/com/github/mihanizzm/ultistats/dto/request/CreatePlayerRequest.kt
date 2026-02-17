package com.github.mihanizzm.ultistats.dto.request

import java.util.UUID

data class CreatePlayerRequest(
    val number: Int?,
    val firstName: String,
    val lastName: String,
    val teamId: UUID? = null,
)
