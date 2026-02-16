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
        teamService.create(TEAM_1)
        teamService.create(TEAM_2)
        matchService.create(MATCH)
        MATCH.events.clear()
        MATCH.diskHolderId = null
    }

    @Test
    fun `В начале матча diskHolder равен null`() {
        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `После подбора диска diskHolder равен игроку, который подобрал`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())

        eventService.create(pickup, MATCH.id)

        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)
    }

    @Test
    fun `После паса diskHolder равен получателю`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val pass = PassEvent(PLAYERS_1[0].id!!, PLAYERS_1[1].id!!, TEAM_1.id, TEAM_1.id, Instant.now())

        eventService.create(pickup, MATCH.id)
        eventService.create(pass, MATCH.id)

        assertEquals(PLAYERS_1[1].id, MATCH.diskHolderId)
    }

    @Test
    fun `После перехвата diskHolder равен перехватившему игроку`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val interception = InterceptionEvent(
            PLAYERS_1[0].id!!, PLAYERS_2[0].id!!,
            TEAM_1.id, TEAM_2.id,
            Instant.now()
        )

        eventService.create(pickup, MATCH.id)
        eventService.create(interception, MATCH.id)

        assertEquals(PLAYERS_2[0].id, MATCH.diskHolderId)
    }

    @Test
    fun `После дропа diskHolder равен null`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val drop = DropEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())

        eventService.create(pickup, MATCH.id)
        eventService.create(drop, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `После блока на маркере diskHolder равен null`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val block = BlockMarkerEvent(
            PLAYERS_1[0].id!!, PLAYERS_2[0].id!!,
            TEAM_1.id, TEAM_2.id,
            Instant.now()
        )

        eventService.create(pickup, MATCH.id)
        eventService.create(block, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `После перебития diskHolder равен null`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val block = BlockFieldEvent(
            PLAYERS_1[0].id!!, PLAYERS_2[0].id!!,
            TEAM_1.id, TEAM_2.id,
            Instant.now()
        )

        eventService.create(pickup, MATCH.id)
        eventService.create(block, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `После гола diskHolder равен null`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val pass = PassEvent(PLAYERS_1[0].id!!, PLAYERS_1[1].id!!, TEAM_1.id, TEAM_1.id, Instant.now())
        val goal = GoalEvent(PLAYERS_1[1].id!!, PLAYERS_1[2].id!!, TEAM_1.id, TEAM_1.id, Instant.now())

        eventService.create(pickup, MATCH.id)
        eventService.create(pass, MATCH.id)
        eventService.create(goal, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `После кэллахана diskHolder равен null`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val callahan = CallahanEvent(
            PLAYERS_1[0].id!!, PLAYERS_2[0].id!!,
            TEAM_1.id, TEAM_2.id,
            Instant.now()
        )

        eventService.create(pickup, MATCH.id)
        eventService.create(callahan, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `Пулл не меняет diskHolder`() {
        val pull = PullEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())

        eventService.create(pull, MATCH.id)

        assertNull(MATCH.diskHolderId)
    }

    @Test
    fun `Таймаут не меняет diskHolder`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val timeoutStart = TimeoutStartEvent(TEAM_1.id, Instant.now())
        val timeoutEnd = TimeoutEndEvent(TEAM_1.id, Instant.now())

        eventService.create(pickup, MATCH.id)
        eventService.create(timeoutStart, MATCH.id)
        eventService.create(timeoutEnd, MATCH.id)

        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)
    }

    @Test
    fun `Халф-тайм не меняет diskHolder`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val halftimeStart = HalftimeStartEvent(Instant.now())
        val halftimeEnd = HalftimeEndEvent(Instant.now())

        eventService.create(pickup, MATCH.id)
        eventService.create(halftimeStart, MATCH.id)
        eventService.create(halftimeEnd, MATCH.id)

        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)
    }

    @Test
    fun `После удаления события diskHolder пересчитывается`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val pass = PassEvent(PLAYERS_1[0].id!!, PLAYERS_1[1].id!!, TEAM_1.id, TEAM_1.id, Instant.now())

        eventService.create(pickup, MATCH.id)
        eventService.create(pass, MATCH.id)
        assertEquals(PLAYERS_1[1].id, MATCH.diskHolderId)

        eventService.remove(1, MATCH.id)

        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)
    }

    @Test
    fun `После редактирования события diskHolder пересчитывается`() {
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val pass = PassEvent(PLAYERS_1[0].id!!, PLAYERS_1[1].id!!, TEAM_1.id, TEAM_1.id, Instant.now())
        val newPass = PassEvent(PLAYERS_1[0].id!!, PLAYERS_1[2].id!!, TEAM_1.id, TEAM_1.id, Instant.now())

        eventService.create(pickup, MATCH.id)
        eventService.create(pass, MATCH.id)
        assertEquals(PLAYERS_1[1].id, MATCH.diskHolderId)

        eventService.edit(1, newPass, MATCH.id)

        assertEquals(PLAYERS_1[2].id, MATCH.diskHolderId)
    }

    @Test
    fun `Полный сценарий поинта - от пулла до гола`() {
        val pull = PullEvent(PLAYERS_2[0].id!!, TEAM_2.id, Instant.now())
        val pickup = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val pass1 = PassEvent(PLAYERS_1[0].id!!, PLAYERS_1[1].id!!, TEAM_1.id, TEAM_1.id, Instant.now())
        val pass2 = PassEvent(PLAYERS_1[1].id!!, PLAYERS_1[2].id!!, TEAM_1.id, TEAM_1.id, Instant.now())
        val goal = GoalEvent(PLAYERS_1[2].id!!, PLAYERS_1[3].id!!, TEAM_1.id, TEAM_1.id, Instant.now())

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
        val pickup1 = TurnoverEvent(PLAYERS_1[0].id!!, TEAM_1.id, Instant.now())
        val block = BlockFieldEvent(
            PLAYERS_1[0].id!!, PLAYERS_2[0].id!!,
            TEAM_1.id, TEAM_2.id,
            Instant.now()
        )
        val pickup2 = TurnoverEvent(PLAYERS_2[1].id!!, TEAM_2.id, Instant.now())

        eventService.create(pickup1, MATCH.id)
        assertEquals(PLAYERS_1[0].id, MATCH.diskHolderId)

        eventService.create(block, MATCH.id)
        assertNull(MATCH.diskHolderId)

        eventService.create(pickup2, MATCH.id)
        assertEquals(PLAYERS_2[1].id, MATCH.diskHolderId)
    }
}
