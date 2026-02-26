package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.MatchAbstractTest
import com.github.mihanizzm.ultistats.model.events.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import java.time.Instant

@Suppress("NonAsciiCharacters")
class DiskHolderTest : MatchAbstractTest() {

    @BeforeEach
    fun setup() {
        MATCH.events.clear()
        MATCH.diskHolderId = null
    }

    @Test
    fun `В начале матча diskHolder равен null`() {
        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `После подбора диска diskHolder равен игроку, который подобрал`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)

        eventService.create(pickup, MATCH.id)

        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)
    }

    @Test
    fun `После паса diskHolder равен получателю`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val pass = TwoPlayerEvent(PLAYERS_1[0].id, PLAYERS_1[1].id, TEAM_1.id, TEAM_1.id, Instant.now(), EventType.PASS)

        eventService.create(pickup, MATCH.id)
        eventService.create(pass, MATCH.id)

        assertEquals(PLAYERS_1[1].id, MATCH.diskHolderId)
    }

    @Test
    fun `После перехвата diskHolder равен перехватившему игроку`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val interception = TwoPlayerEvent(
            PLAYERS_1[0].id, PLAYERS_2[0].id,
            TEAM_1.id, TEAM_2.id,
            Instant.now(), EventType.INTERCEPTION
        )

        eventService.create(pickup, MATCH.id)
        eventService.create(interception, MATCH.id)

        assertEquals(PLAYERS_2[0].id, MATCH.diskHolderId)
    }

    @Test
    fun `После дропа diskHolder равен null`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val drop = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.DROP)

        eventService.create(pickup, MATCH.id)
        eventService.create(drop, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `После блока на маркере diskHolder равен null`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val block = TwoPlayerEvent(
            PLAYERS_1[0].id, PLAYERS_2[0].id,
            TEAM_1.id, TEAM_2.id,
            Instant.now(), EventType.BLOCK_MARKER
        )

        eventService.create(pickup, MATCH.id)
        eventService.create(block, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `После перебития diskHolder равен null`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val block = TwoPlayerEvent(
            PLAYERS_1[0].id, PLAYERS_2[0].id,
            TEAM_1.id, TEAM_2.id,
            Instant.now(), EventType.BLOCK_FIELD
        )

        eventService.create(pickup, MATCH.id)
        eventService.create(block, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `После гола diskHolder равен null`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val pass = TwoPlayerEvent(PLAYERS_1[0].id, PLAYERS_1[1].id, TEAM_1.id, TEAM_1.id, Instant.now(), EventType.PASS)
        val goal = TwoPlayerEvent(PLAYERS_1[1].id, PLAYERS_1[2].id, TEAM_1.id, TEAM_1.id, Instant.now(), EventType.GOAL)

        eventService.create(pickup, MATCH.id)
        eventService.create(pass, MATCH.id)
        eventService.create(goal, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `После кэллахана diskHolder равен null`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val callahan = TwoPlayerEvent(
            PLAYERS_1[0].id, PLAYERS_2[0].id,
            TEAM_1.id, TEAM_2.id,
            Instant.now(), EventType.CALLAHAN
        )

        eventService.create(pickup, MATCH.id)
        eventService.create(callahan, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `Пулл не меняет diskHolder`() {
        val pull = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.PULL)

        eventService.create(pull, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `Таймаут не меняет diskHolder`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val timeoutStart = TeamEvent(TEAM_1.id, Instant.now(), EventType.TIMEOUT_START)
        val timeoutEnd = TeamEvent(TEAM_1.id, Instant.now(), EventType.TIMEOUT_END)

        eventService.create(pickup, MATCH.id)
        eventService.create(timeoutStart, MATCH.id)
        eventService.create(timeoutEnd, MATCH.id)

        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)
    }

    @Test
    fun `Халф-тайм не меняет diskHolder`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val halftimeStart = SystemEvent(Instant.now(), EventType.HALFTIME_START)
        val halftimeEnd = SystemEvent(Instant.now(), EventType.HALFTIME_END)

        eventService.create(pickup, MATCH.id)
        eventService.create(halftimeStart, MATCH.id)
        eventService.create(halftimeEnd, MATCH.id)

        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)
    }

    @Test
    fun `После удаления события diskHolder пересчитывается`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val pass = TwoPlayerEvent(PLAYERS_1[0].id, PLAYERS_1[1].id, TEAM_1.id, TEAM_1.id, Instant.now(), EventType.PASS)

        eventService.create(pickup, MATCH.id)
        eventService.create(pass, MATCH.id)
        assertEquals(PLAYERS_1[1].id, MATCH.diskHolderId)

        eventService.remove(1, MATCH.id)

        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)
    }

    @Test
    fun `После редактирования события diskHolder пересчитывается`() {
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val pass = TwoPlayerEvent(PLAYERS_1[0].id, PLAYERS_1[1].id, TEAM_1.id, TEAM_1.id, Instant.now(), EventType.PASS)
        val newPass = TwoPlayerEvent(PLAYERS_1[0].id, PLAYERS_1[2].id, TEAM_1.id, TEAM_1.id, Instant.now(), EventType.PASS)

        eventService.create(pickup, MATCH.id)
        eventService.create(pass, MATCH.id)
        assertEquals(PLAYERS_1[1].id, MATCH.diskHolderId)

        eventService.edit(1, newPass, MATCH.id)

        assertEquals(PLAYERS_1[2].id, MATCH.diskHolderId)
    }

    @Test
    fun `Полный сценарий поинта - от пулла до гола`() {
        val pull = OnePlayerEvent(PLAYERS_2[0].id, TEAM_2.id, Instant.now(), EventType.PULL)
        val pickup = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val pass1 = TwoPlayerEvent(PLAYERS_1[0].id, PLAYERS_1[1].id, TEAM_1.id, TEAM_1.id, Instant.now(), EventType.PASS)
        val pass2 = TwoPlayerEvent(PLAYERS_1[1].id, PLAYERS_1[2].id, TEAM_1.id, TEAM_1.id, Instant.now(), EventType.PASS)
        val goal = TwoPlayerEvent(PLAYERS_1[2].id, PLAYERS_1[3].id, TEAM_1.id, TEAM_1.id, Instant.now(), EventType.GOAL)

        eventService.create(pull, MATCH.id)
        assertNull(MATCH.diskHolderId)

        eventService.create(pickup, MATCH.id)
        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)

        eventService.create(pass1, MATCH.id)
        assertEquals(PLAYERS_1[1].id, MATCH.diskHolderId)

        eventService.create(pass2, MATCH.id)
        assertEquals(PLAYERS_1[2].id, MATCH.diskHolderId)

        eventService.create(goal, MATCH.id)
        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `Сценарий с блоком и подбором`() {
        val pickup1 = OnePlayerEvent(PLAYERS_1[0].id, TEAM_1.id, Instant.now(), EventType.TURNOVER)
        val block = TwoPlayerEvent(
            PLAYERS_1[0].id, PLAYERS_2[0].id,
            TEAM_1.id, TEAM_2.id,
            Instant.now(), EventType.BLOCK_FIELD
        )
        val pickup2 = OnePlayerEvent(PLAYERS_2[1].id, TEAM_2.id, Instant.now(), EventType.TURNOVER)

        eventService.create(pickup1, MATCH.id)
        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)

        eventService.create(block, MATCH.id)
        assertNull(MATCH.diskHolderId)

        eventService.create(pickup2, MATCH.id)
        assertEquals(PLAYERS_2[1].id, MATCH.diskHolderId)
    }
}
