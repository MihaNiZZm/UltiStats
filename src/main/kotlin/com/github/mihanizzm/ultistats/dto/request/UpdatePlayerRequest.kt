package com.github.mihanizzm.ultistats.dto.request

import java.util.UUID

/**
 * DTO для частичного обновления игрока.
 * Все поля необязательны — обновляются только переданные поля.
 */
data class UpdatePlayerRequest(
    val number: Int? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val teamId: UUID? = null,
)
