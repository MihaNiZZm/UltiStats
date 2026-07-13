package com.github.mihanizzm.ultistats

import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.factory.EventFactory
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.TeamPlayer
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataTeamPlayerRepository
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamPlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@Transactional
class RelationalModelIntegrationTest {
    @Autowired lateinit var teamService: TeamService
    @Autowired lateinit var playerService: PlayerService
    @Autowired lateinit var teamPlayerService: TeamPlayerService
    @Autowired lateinit var matchService: MatchService
    @Autowired lateinit var eventService: EventService
    @Autowired lateinit var eventFactory: EventFactory
    @Autowired lateinit var teamPlayerRepository: SpringDataTeamPlayerRepository

    private lateinit var firstTeam: Team
    private lateinit var secondTeam: Team
    private lateinit var firstPlayer: Player
    private lateinit var secondPlayer: Player
    private lateinit var match: Match

    @BeforeEach
    fun setUp() {
        firstTeam = Team(UUID.randomUUID(), "First", emptyList())
        secondTeam = Team(UUID.randomUUID(), "Second", emptyList())
        teamService.create(firstTeam)
        teamService.create(secondTeam)

        firstPlayer = Player(UUID.randomUUID(), firstTeam.id, 7, "First", "Player")
        secondPlayer = Player(UUID.randomUUID(), firstTeam.id, 11, "Second", "Player")
        playerService.create(firstPlayer)
        playerService.create(secondPlayer)

        match = Match(UUID.randomUUID(), listOf(firstTeam.id, secondTeam.id))
        matchService.create(match)
    }

    @Test
    fun `player number is unique inside a team`() {
        val anotherPlayer = Player(UUID.randomUUID(), null, null, "Another", "Player")
        playerService.create(anotherPlayer)

        assertThrows<DataIntegrityViolationException> {
            teamPlayerRepository.saveAndFlush(TeamPlayer(firstTeam.id, anotherPlayer.id, 7))
        }
    }

    @Test
    fun `match roster remains unchanged after current roster transfer`() {
        teamPlayerService.remove(firstTeam.id, firstPlayer.id)
        teamPlayerService.add(secondTeam.id, firstPlayer.id, 19)

        val storedMatch = matchService.getOrThrow(match.id)

        assertEquals(setOf(firstPlayer.id, secondPlayer.id), storedMatch.playerIdsByTeam[firstTeam.id]?.toSet())
        assertEquals(emptyList(), storedMatch.playerIdsByTeam[secondTeam.id].orEmpty())
    }

    @Test
    fun `match team order is preserved by position`() {
        assertEquals(listOf(firstTeam.id, secondTeam.id), matchService.getOrThrow(match.id).teamIds)
    }

    @Test
    fun `cached score follows active goal events`() {
        val goal = TwoPlayerEvent(
            firstPlayer.id,
            secondPlayer.id,
            firstTeam.id,
            firstTeam.id,
            Instant.parse("2026-07-13T12:00:00Z"),
            EventType.GOAL,
        )

        eventService.create(goal, match.id)
        assertEquals(1, matchService.getOrThrow(match.id).teamScores.single { it.teamId == firstTeam.id }.score)

        eventService.remove(0, match.id)
        assertEquals(0, matchService.getOrThrow(match.id).teamScores.single { it.teamId == firstTeam.id }.score)
    }

    @Test
    fun `player added after match creation is rejected by event factory`() {
        val latePlayer = Player(UUID.randomUUID(), firstTeam.id, 21, "Late", "Player")
        playerService.create(latePlayer)

        val event = eventFactory.createFromRequest(
            CreateEventRequest(
                type = EventType.TURNOVER,
                timestamp = Instant.parse("2026-07-13T12:00:00Z"),
                playerId = latePlayer.id,
            ),
            match.id,
        )

        assertNull(event)
    }
}
