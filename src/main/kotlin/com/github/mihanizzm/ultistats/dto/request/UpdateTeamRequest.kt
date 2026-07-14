package com.github.mihanizzm.ultistats.dto.request

/**
 * DTO для частичного обновления команды.
 * Все поля необязательны — обновляются только переданные поля.
 */
data class UpdateTeamRequest(
    val name: String? = null,
    val city: String? = null,
)
