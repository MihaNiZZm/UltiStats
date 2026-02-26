package com.github.mihanizzm.ultistats.dto.request

import java.util.UUID

data class PlayerFilterRequest(
    val name: String? = null,
    val teamId: UUID? = null,
)
