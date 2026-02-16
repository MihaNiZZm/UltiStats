package com.github.mihanizzm.ultistats.dto.request

import java.util.UUID

data class CreateMatchRequest(
    val teamIds: List<UUID>,
)
