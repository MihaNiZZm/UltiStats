package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.MatchAbstractTest
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.service.result.EventCommandResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Suppress("NonAsciiCharacters")
class EventServiceImplTest : MatchAbstractTest() {

    @BeforeEach
    fun setup() {
        MATCH.events.clear()
    }

    @Test
    fun `Событие регистрируется`() {
        val event = OnePlayerEvent(PLAYERS_1[0].id, EVENT_AT, EventType.PULL)

        assertIs<EventCommandResult.Success>(eventService.create(event, MATCH.id))

        val match = matchService.getOrThrow(MATCH.id)
        assertEquals(1, match.events.size)
        assertEquals(event, match.events[0])
    }

    @Test
    fun `Пас не может быть первым игровым событием`() {
        val pass = TwoPlayerEvent(PLAYERS_1[0].id, PLAYERS_1[1].id, EVENT_AT, EventType.PASS)

        val result = assertIs<EventCommandResult.Conflict>(eventService.create(pass, MATCH.id))

        assertEquals("EVENT_SEQUENCE_VIOLATION", result.problem.code.name)
        assertEquals(emptyList(), matchService.getOrThrow(MATCH.id).events)
    }

    @Test
    fun `Пулл подбор и гол образуют допустимый поинт`() {
        val events = listOf(
            OnePlayerEvent(PLAYERS_2[0].id, EVENT_AT, EventType.PULL),
            OnePlayerEvent(PLAYERS_1[0].id, EVENT_AT.plusSeconds(1), EventType.PICKUP),
            TwoPlayerEvent(PLAYERS_1[0].id, PLAYERS_1[1].id, EVENT_AT.plusSeconds(2), EventType.GOAL),
        )

        events.forEach { assertIs<EventCommandResult.Success>(eventService.create(it, MATCH.id)) }

        assertEquals(events, matchService.getOrThrow(MATCH.id).events)
    }

    @Test
    fun `Событие изменяется`() {
        val event = OnePlayerEvent(PLAYERS_1[0].id, EVENT_AT, EventType.PULL)
        val newEvent = OnePlayerEvent(PLAYERS_1[1].id, event.occurredAt, EventType.PULL)

        val stored = assertIs<EventCommandResult.Success>(eventService.create(event, MATCH.id)).event
        assertIs<EventCommandResult.Success>(eventService.update(stored.id, MATCH.id) { newEvent })

        val match = matchService.getOrThrow(MATCH.id)
        assertEquals(1, match.events.size)
        assertEquals(newEvent, match.events[0])
    }

    @Test
    fun `Изменение отсутствующего события возвращает NotFound`() {
        val event = OnePlayerEvent(PLAYERS_1[0].id, EVENT_AT, EventType.PULL)

        assertIs<EventCommandResult.NotFound>(eventService.update(java.util.UUID.randomUUID(), MATCH.id) { event })
    }

    @Test
    fun `Событие удаляется`() {
        val event = OnePlayerEvent(PLAYERS_1[0].id, EVENT_AT, EventType.PULL)

        val stored = assertIs<EventCommandResult.Success>(eventService.create(event, MATCH.id)).event
        assertIs<EventCommandResult.Deleted>(eventService.remove(stored.id, MATCH.id))

        val match = matchService.getOrThrow(MATCH.id)
        assertEquals(0, match.events.size)
    }

    @Test
    fun `Удаление обязательного подбора отклоняется без изменения журнала`() {
        val pull = assertIs<EventCommandResult.Success>(
            eventService.create(OnePlayerEvent(PLAYERS_2[0].id, EVENT_AT, EventType.PULL), MATCH.id),
        ).event
        val pickup = assertIs<EventCommandResult.Success>(
            eventService.create(
                OnePlayerEvent(PLAYERS_1[0].id, EVENT_AT.plusSeconds(1), EventType.PICKUP),
                MATCH.id,
            ),
        ).event
        val pass = assertIs<EventCommandResult.Success>(
            eventService.create(
                TwoPlayerEvent(
                    PLAYERS_1[0].id,
                    PLAYERS_1[1].id,
                    EVENT_AT.plusSeconds(2),
                    EventType.PASS,
                ),
                MATCH.id,
            ),
        ).event

        val result = assertIs<EventCommandResult.Conflict>(eventService.remove(pickup.id, MATCH.id))

        assertEquals("EVENT_SEQUENCE_VIOLATION", result.problem.code.name)
        assertEquals(listOf(pull.id, pickup.id, pass.id), eventService.getAllEventsOfMatch(MATCH.id).map { it.id })
    }

    @Test
    fun `Удаление финального гола завершенного матча отклоняется`() {
        eventService.create(OnePlayerEvent(PLAYERS_2[0].id, EVENT_AT, EventType.PULL), MATCH.id)
        eventService.create(OnePlayerEvent(PLAYERS_1[0].id, EVENT_AT.plusSeconds(1), EventType.PICKUP), MATCH.id)
        val goal = assertIs<EventCommandResult.Success>(
            eventService.create(
                TwoPlayerEvent(
                    PLAYERS_1[0].id,
                    PLAYERS_1[1].id,
                    EVENT_AT.plusSeconds(2),
                    EventType.GOAL,
                ),
                MATCH.id,
            ),
        ).event
        matchService.endMatch(MATCH.id, EVENT_AT.plusSeconds(3))

        val result = assertIs<EventCommandResult.Conflict>(eventService.remove(goal.id, MATCH.id))

        assertEquals("EVENT_SEQUENCE_VIOLATION", result.problem.code.name)
        assertEquals(EventType.GOAL, eventService.get(goal.id, MATCH.id)?.event?.type)
    }

    @Test
    fun `Удаление отсутствующего события возвращает NotFound`() {
        assertIs<EventCommandResult.NotFound>(eventService.remove(java.util.UUID.randomUUID(), MATCH.id))
    }

    @Test
    fun `Выводится список всех событий`() {
        val events = listOf(
            OnePlayerEvent(PLAYERS_2[0].id, EVENT_AT, EventType.PULL),
            OnePlayerEvent(PLAYERS_1[0].id, EVENT_AT.plusSeconds(1), EventType.PICKUP),
            TwoPlayerEvent(PLAYERS_1[0].id, PLAYERS_1[1].id, EVENT_AT.plusSeconds(2), EventType.PASS),
            TwoPlayerEvent(PLAYERS_1[1].id, PLAYERS_1[2].id, EVENT_AT.plusSeconds(3), EventType.PASS),
            TwoPlayerEvent(PLAYERS_1[2].id, PLAYERS_1[3].id, EVENT_AT.plusSeconds(4), EventType.PASS),
            TwoPlayerEvent(PLAYERS_1[3].id, PLAYERS_1[4].id, EVENT_AT.plusSeconds(5), EventType.GOAL),
        )

        events.forEach { assertIs<EventCommandResult.Success>(eventService.create(it, MATCH.id)) }

        assertEquals(events, eventService.getAllEventsOfMatch(MATCH.id).map { it.event })
    }

    companion object {
        private val EVENT_AT = Instant.parse("2026-08-14T10:00:00Z")
    }
}
