package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.MatchAbstractTest
import com.github.mihanizzm.ultistats.model.events.GoalEvent
import com.github.mihanizzm.ultistats.model.events.PassEvent
import com.github.mihanizzm.ultistats.model.events.PullEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals

@Suppress("NonAsciiCharacters")
class EventServiceImplTest : MatchAbstractTest() {

    @BeforeEach
    fun setup() {
        MATCH.events.clear()
    }

    @Test
    fun `Событие регистрируется`() {
        val event = PullEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now())

        eventService.create(event, MATCH.id)

        assertEquals(1, MATCH.events.size)
        assertEquals(event, MATCH.events[0])
    }

    @Test
    fun `Событие изменяется`() {
        val event = PullEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now())
        val newEvent = PullEvent(PLAYERS_1[1].id, TEAM_1.id, Instant.now())

        eventService.create(event, MATCH.id)
        eventService.edit(0, newEvent, MATCH.id)

        assertEquals(1, MATCH.events.size)
        assertEquals(newEvent, MATCH.events[0])
    }

    @Test
    fun `Получаем исключение при изменении, если события с таким индексом не существует`() {
        val event = PullEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now())

        assertThrows<IllegalArgumentException> { eventService.edit(0, event, MATCH.id) }
    }

    @Test
    fun `Событие удаляется`() {
        val event = PullEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now())

        eventService.create(event, MATCH.id)
        eventService.remove(0, MATCH.id)

        assertEquals(0, MATCH.events.size)
    }

    @Test
    fun `Получаем исключение при удалении, если события с таким индексом не существует`() {
        assertThrows<IllegalArgumentException> { eventService.remove(0, MATCH.id) }
    }

    @Test
    fun `Выводится список всех событий`() {
        val events = listOf(
            PullEvent(PLAYERS_2[0].id, TEAM_2.id, Instant.now()),
            PassEvent(PLAYERS_1[0].id, PLAYERS_1[1].id, TEAM_1.id, TEAM_1.id, Instant.now()),
            PassEvent(PLAYERS_1[1].id, PLAYERS_1[2].id, TEAM_1.id, TEAM_1.id, Instant.now()),
            PassEvent(PLAYERS_1[2].id, PLAYERS_1[3].id, TEAM_1.id, TEAM_1.id, Instant.now()),
            GoalEvent(PLAYERS_1[3].id, PLAYERS_1[4].id, TEAM_1.id, TEAM_1.id, Instant.now()),
        )

        events.forEach { eventService.create(it, MATCH.id) }

        assertEquals(events, eventService.getAllEventsOfMatch(MATCH.id))
    }
}
