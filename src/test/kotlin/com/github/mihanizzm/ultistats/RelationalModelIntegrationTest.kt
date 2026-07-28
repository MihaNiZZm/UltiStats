package com.github.mihanizzm.ultistats

import com.github.mihanizzm.ultistats.dto.request.OnePlayerEventRequest
import com.github.mihanizzm.ultistats.dto.request.TwoPlayerEventRequest
import com.github.mihanizzm.ultistats.factory.EventFactory
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.MatchParticipantKind
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
import com.github.mihanizzm.ultistats.service.statistics.StatisticsService
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
import kotlin.test.assertNotNull
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
    @Autowired lateinit var statisticsService: StatisticsService
    @Autowired lateinit var teamPlayerRepository: SpringDataTeamPlayerRepository

    private lateinit var firstTeam: Team
    private lateinit var secondTeam: Team
    private lateinit var firstPlayer: Player
    private lateinit var secondPlayer: Player
    private lateinit var match: Match

    @BeforeEach
    fun setUp() {
        firstTeam = Team(UUID.randomUUID(), "First")
        secondTeam = Team(UUID.randomUUID(), "Second")
        teamService.create(firstTeam)
        teamService.create(secondTeam)

        firstPlayer = Player(UUID.randomUUID(), "First", "Player")
        secondPlayer = Player(UUID.randomUUID(), "Second", "Player")
        playerService.create(firstPlayer)
        playerService.create(secondPlayer)
        teamPlayerService.add(firstTeam.id, firstPlayer.id, 7)
        teamPlayerService.add(firstTeam.id, secondPlayer.id, 11)

        match = Match(UUID.randomUUID(), listOf(firstTeam.id, secondTeam.id))
        matchService.create(match)
    }

    @Test
    fun `player number is unique inside a team`() {
        val anotherPlayer = Player(UUID.randomUUID(), "Another", "Player")
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

        val firstTeamPlayers = storedMatch.participantsByTeam[firstTeam.id].orEmpty()
            .filter { it.kind == MatchParticipantKind.PLAYER }
            .mapNotNull { it.playerId }
        val secondTeamPlayers = storedMatch.participantsByTeam[secondTeam.id].orEmpty()
            .filter { it.kind == MatchParticipantKind.PLAYER }
            .mapNotNull { it.playerId }

        assertEquals(setOf(firstPlayer.id, secondPlayer.id), firstTeamPlayers.toSet())
        assertEquals(emptyList(), secondTeamPlayers)
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
            Instant.parse("2026-07-13T12:00:00Z"),
            EventType.GOAL,
        )

        eventService.create(goal, match.id)
        assertEquals(1, matchService.getOrThrow(match.id).teamScores.single { it.teamId == firstTeam.id }.score)

        val stored = eventService.getAllEventsOfMatch(match.id).single()
        eventService.remove(stored.id, match.id)
        assertEquals(0, matchService.getOrThrow(match.id).teamScores.single { it.teamId == firstTeam.id }.score)
    }

    @Test
    fun `player added after match creation is rejected by event factory`() {
        val latePlayer = Player(UUID.randomUUID(), "Late", "Player")
        playerService.create(latePlayer)

        val event = eventFactory.createFromRequest(
            OnePlayerEventRequest(
                type = EventType.TURNOVER,
                occurredAt = Instant.parse("2026-07-13T12:00:00Z"),
                participantId = latePlayer.id,
            ),
            match.id,
        )

        assertNull(event)
    }

    @Test
    fun `match snapshot contains two unknown participants per team`() {
        val storedMatch = matchService.getOrThrow(match.id)
        val unknownParticipantIds = mutableSetOf<UUID>()

        storedMatch.teamIds.forEach { teamId ->
            val unknowns = storedMatch.participantsByTeam.getValue(teamId)
                .filter { it.kind == MatchParticipantKind.UNKNOWN }

            assertEquals(listOf(1, 2), unknowns.map { it.unknownSlot })
            assertEquals(listOf(null, null), unknowns.map { it.playerId })
            unknownParticipantIds += unknowns.map { it.participantId }
        }
        assertEquals(emptySet(), playerService.getAll().map { it.id }.toSet().intersect(unknownParticipantIds))
    }

    @Test
    fun `pass between two unknown participants contributes to their statistics`() {
        val unknowns = matchService.getOrThrow(match.id).participantsByTeam.getValue(firstTeam.id)
            .filter { it.kind == MatchParticipantKind.UNKNOWN }
        val event = eventFactory.createFromRequest(
            TwoPlayerEventRequest(
                type = EventType.PASS,
                occurredAt = Instant.parse("2026-07-13T12:00:00Z"),
                fromParticipantId = unknowns[0].participantId,
                toParticipantId = unknowns[1].participantId,
            ),
            match.id,
        )

        eventService.create(assertNotNull(event), match.id)
        val statistics = statisticsService.recalculateMatchStatistics(match.id)

        assertEquals(
            1,
            statistics.playerStatistics.single { it.participantId == unknowns[0].participantId }.attack.passes,
        )
        assertEquals(
            1,
            statistics.playerStatistics.single { it.participantId == unknowns[1].participantId }.attack.catches,
        )
    }
}
