package com.github.mihanizzm.ultistats.util

import com.github.mihanizzm.ultistats.dto.common.SortDirection
import com.github.mihanizzm.ultistats.dto.common.SortParam
import com.github.mihanizzm.ultistats.util.SortingUtils.applySorting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@Suppress("NonAsciiCharacters")
class SortingUtilsTest {

    data class TestItem(val name: String, val value: Int)

    private val extractors: Map<String, (TestItem) -> Comparable<*>?> = mapOf(
        "name" to { it.name },
        "value" to { it.value },
    )

    @Test
    fun `Сортировка по возрастанию работает`() {
        val items = listOf(
            TestItem("C", 3),
            TestItem("A", 1),
            TestItem("B", 2),
        )

        val sorted = items.applySorting(SortParam("name", SortDirection.ASC), extractors)

        assertEquals("A", sorted[0].name)
        assertEquals("B", sorted[1].name)
        assertEquals("C", sorted[2].name)
    }

    @Test
    fun `Сортировка по убыванию работает`() {
        val items = listOf(
            TestItem("A", 1),
            TestItem("B", 2),
            TestItem("C", 3),
        )

        val sorted = items.applySorting(SortParam("name", SortDirection.DESC), extractors)

        assertEquals("C", sorted[0].name)
        assertEquals("B", sorted[1].name)
        assertEquals("A", sorted[2].name)
    }

    @Test
    fun `Парсинг DESC работает`() {
        val param = SortParam.parse("name:desc")

        assertEquals("name", param.field)
        assertEquals(SortDirection.DESC, param.direction)
    }

    @Test
    fun `Парсинг ASC работает`() {
        val param = SortParam.parse("name:asc")

        assertEquals("name", param.field)
        assertEquals(SortDirection.ASC, param.direction)
    }

    @Test
    fun `Парсинг без направления возвращает ASC`() {
        val param = SortParam.parse("name")

        assertEquals("name", param.field)
        assertEquals(SortDirection.ASC, param.direction)
    }
}
