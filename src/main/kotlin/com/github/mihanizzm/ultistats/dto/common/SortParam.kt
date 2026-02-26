package com.github.mihanizzm.ultistats.dto.common

data class SortParam(
    val field: String,
    val direction: SortDirection = SortDirection.ASC,
) {
    companion object {
        /**
         * Парсит строку вида "lastName:desc" или "lastName" (asc по умолчанию).
         */
        fun parse(param: String): SortParam {
            val parts = param.split(":")
            val field = parts[0].trim()
            val direction = if (parts.size > 1) {
                SortDirection.fromString(parts[1].trim())
            } else {
                SortDirection.ASC
            }
            return SortParam(field, direction)
        }
    }
}

enum class SortDirection {
    ASC,
    DESC;

    companion object {
        fun fromString(value: String): SortDirection =
            when (value.lowercase()) {
                "desc" -> DESC
                else -> ASC
            }
    }
}
