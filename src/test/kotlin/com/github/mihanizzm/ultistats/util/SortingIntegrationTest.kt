package com.github.mihanizzm.ultistats.util

import com.github.mihanizzm.ultistats.dto.common.SortDirection
import com.github.mihanizzm.ultistats.dto.common.SortParam
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.util.SortingUtils.applySorting
import com.github.mihanizzm.ultistats.facade.PlayerFacade
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

@Suppress("NonAsciiCharacters")
class SortingIntegrationTest {

    @Test
    fun `Сортировка игроков по убыванию фамилии работает`() {
        val players = listOf(
            Player(UUID.randomUUID(), "Игрок", "Альфа"),
            Player(UUID.randomUUID(), "Игрок", "Бета"),
            Player(UUID.randomUUID(), "Игрок", "Гамма"),
        )

        val sortParam = SortParam("lastName", SortDirection.DESC)
        val sorted = players.applySorting(sortParam, PlayerFacade.SORT_FIELD_EXTRACTORS)

        // Гамма > Бета > Альфа в Unicode, так что DESC должен дать Гамма первым
        assertEquals("Гамма", sorted[0].lastName)
        assertEquals("Бета", sorted[1].lastName)
        assertEquals("Альфа", sorted[2].lastName)
    }

    @Test
    fun `SortParam парсит desc из строки`() {
        val param = SortParam.parse("lastName:desc")

        assertEquals("lastName", param.field)
        assertEquals(SortDirection.DESC, param.direction)
    }
}
