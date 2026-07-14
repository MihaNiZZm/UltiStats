package com.github.mihanizzm.ultistats.dto.request

/**
 * DTO для частичного обновления игрока.
 * Все поля необязательны — обновляются только переданные поля.
 */
data class UpdatePlayerRequest(
    val firstName: String? = null,
    val lastName: String? = null,
)
