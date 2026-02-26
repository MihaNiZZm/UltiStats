package com.github.mihanizzm.ultistats.dto.common

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int,
) {
    companion object {
        fun <T> of(
            content: List<T>,
            totalElements: Long,
            page: Int,
            size: Int,
        ): PageResponse<T> {
            val totalPages = if (size > 0) {
                ((totalElements + size - 1) / size).toInt()
            } else {
                0
            }
            return PageResponse(
                content = content,
                totalElements = totalElements,
                totalPages = totalPages,
                currentPage = page,
                size = size,
            )
        }
    }
}
