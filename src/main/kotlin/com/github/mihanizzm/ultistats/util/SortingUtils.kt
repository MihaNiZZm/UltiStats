package com.github.mihanizzm.ultistats.util

import com.github.mihanizzm.ultistats.dto.common.SortDirection
import com.github.mihanizzm.ultistats.dto.common.SortParam

object SortingUtils {
    /**
     * Создаёт компаратор на основе параметра сортировки.
     *
     * @param sortParam параметр сортировки (например, ("lastName", DESC))
     * @param fieldExtractors маппинг имени поля на функцию извлечения значения
     * @return Comparator для сортировки или null, если поле не найдено
     */
    fun <T> buildComparator(
        sortParam: SortParam,
        fieldExtractors: Map<String, (T) -> Comparable<*>?>,
    ): Comparator<T>? {
        val extractor = fieldExtractors[sortParam.field] ?: return null

        @Suppress("UNCHECKED_CAST")
        val fieldComparator = compareBy<T, Comparable<Any>?>(
            nullsLast()
        ) { extractor(it) as? Comparable<Any> }

        return if (sortParam.direction == SortDirection.DESC) {
            fieldComparator.reversed()
        } else {
            fieldComparator
        }
    }

    /**
     * Применяет сортировку к списку.
     */
    fun <T> List<T>.applySorting(
        sortParam: SortParam,
        fieldExtractors: Map<String, (T) -> Comparable<*>?>,
    ): List<T> {
        val comparator = buildComparator(sortParam, fieldExtractors)
        return if (comparator != null) {
            this.sortedWith(comparator)
        } else {
            this
        }
    }
}
