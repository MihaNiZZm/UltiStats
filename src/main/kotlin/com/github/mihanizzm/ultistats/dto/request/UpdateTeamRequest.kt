package com.github.mihanizzm.ultistats.dto.request

import java.util.UUID

/**
 * DTO для частичного обновления команды.
 * Все поля необязательны — обновляются только переданные поля.
 */
data class UpdateTeamRequest(
    val name: String? = null,
    val playerIds: List<UUID>? = null,
    val city: String? = null,
)
