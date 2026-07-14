package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.MatchAbstractTest
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

@Suppress("NonAsciiCharacters")
class EventServiceImplTest : MatchAbstractTest() {

    private fun now() = Instant.now().truncatedTo(ChronoUnit.MICROS)

    @BeforeEach
    fun setup() {
        MATCH.events.clear()
        matchService.update(MATCH)
    }

    @Test
    fun `Событие регистрируется`() {
        val event = OnePlayerEvent(PLAYERS_1[0].id, now(), EventType.PULL)

        eventService.create(event, MATCH.id)

        val match = matchService.getOrThrow(MATCH.id)
        assertEquals(1, match.events.size)
        assertEquals(event, match.events[0])
    }

    @Test
    fun `Событие изменяется`() {
        val event = OnePlayerEvent(PLAYERS_1[0].id, now(), EventType.PULL)
        val newEvent = OnePlayerEvent(PLAYERS_1[1].id, event.occurredAt, EventType.PULL)

        val stored = eventService.create(event, MATCH.id)
        eventService.update(stored.id, newEvent, MATCH.id)

        val match = matchService.getOrThrow(MATCH.id)
        assertEquals(1, match.events.size)
        assertEquals(newEvent, match.events[0])
    }

    @Test
    fun `Получаем исключение при изменении, если события с таким индексом не существует`() {
        val event = OnePlayerEvent(PLAYERS_1[0].id, now(), EventType.PULL)

        assertThrows<IllegalArgumentException> { eventService.update(java.util.UUID.randomUUID(), event, MATCH.id) }
    }

    @Test
    fun `Событие удаляется`() {
        val event = OnePlayerEvent(PLAYERS_1[0].id, now(), EventType.PULL)

        val stored = eventService.create(event, MATCH.id)
        eventService.remove(stored.id, MATCH.id)

        val match = matchService.getOrThrow(MATCH.id)
        assertEquals(0, match.events.size)
    }

    @Test
    fun `Удаление отсутствующего события возвращает false`() {
        assertEquals(false, eventService.remove(java.util.UUID.randomUUID(), MATCH.id))
    }

    @Test
    fun `Выводится список всех событий`() {
        val events = listOf(
            OnePlayerEvent(PLAYERS_2[0].id, now(), EventType.PULL),
            TwoPlayerEvent(PLAYERS_1[0].id, PLAYERS_1[1].id, now(), EventType.PASS),
            TwoPlayerEvent(PLAYERS_1[1].id, PLAYERS_1[2].id, now(), EventType.PASS),
            TwoPlayerEvent(PLAYERS_1[2].id, PLAYERS_1[3].id, now(), EventType.PASS),
            TwoPlayerEvent(PLAYERS_1[3].id, PLAYERS_1[4].id, now(), EventType.GOAL),
        )

        events.forEach { eventService.create(it, MATCH.id) }

        assertEquals(events, eventService.getAllEventsOfMatch(MATCH.id).map { it.event })
    }
}
