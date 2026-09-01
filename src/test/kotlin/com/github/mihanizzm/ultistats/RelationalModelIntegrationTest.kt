package com.github.mihanizzm.ultistats

import com.github.mihanizzm.ultistats.dto.request.OnePlayerEventRequest
import com.github.mihanizzm.ultistats.dto.request.TwoPlayerEventRequest
import com.github.mihanizzm.ultistats.factory.EventFactory
import com.github.mihanizzm.ultistats.fixture.MatchEventTestFixture
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.MatchParticipantKind
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.TeamPlayer
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataTeamPlayerRepository
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamPlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import com.github.mihanizzm.ultistats.service.result.MatchCommandResult
import com.github.mihanizzm.ultistats.service.statistics.StatisticsService
import com.github.mihanizzm.ultistats.validation.match.MatchProblemCode
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@Transactional
class RelationalModelIntegrationTest {
    private val matchEventFixture by lazy { MatchEventTestFixture(matchService, eventService) }

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
            .map { it.participantId }
        val secondTeamPlayers = storedMatch.participantsByTeam[secondTeam.id].orEmpty()
            .filter { it.kind == MatchParticipantKind.PLAYER }
            .map { it.participantId }

        assertEquals(setOf(firstPlayer.id, secondPlayer.id), firstTeamPlayers.toSet())
        assertEquals(emptyList(), secondTeamPlayers)
    }

    @Test
    fun `match team order is preserved by position`() {
        assertEquals(listOf(firstTeam.id, secondTeam.id), matchService.getOrThrow(match.id).teamIds)
    }

    @Test
    fun `cached score follows active goal events`() {
        val goalAt = Instant.parse("2026-07-13T12:00:00Z")
        val goal = TwoPlayerEvent(
            firstPlayer.id,
            secondPlayer.id,
            goalAt,
            EventType.GOAL,
        )
        assertIs<MatchCommandResult.Success<Match>>(
            matchService.startMatch(match.id, Instant.parse("2026-07-13T11:00:00Z")),
        )

        eventService.create(OnePlayerEvent(firstPlayer.id, goalAt.minusSeconds(2), EventType.PULL), match.id)
        eventService.create(OnePlayerEvent(secondPlayer.id, goalAt.minusSeconds(1), EventType.PICKUP), match.id)
        eventService.create(goal, match.id)
        assertEquals(1, matchService.getOrThrow(match.id).teamScores.single { it.teamId == firstTeam.id }.score)

        val stored = eventService.getAllEventsOfMatch(match.id).single { it.event.type == EventType.GOAL }
        eventService.remove(stored.id, match.id)
        assertEquals(0, matchService.getOrThrow(match.id).teamScores.single { it.teamId == firstTeam.id }.score)
    }

    @Test
    fun `player added after match creation is rejected by event factory`() {
        val latePlayer = Player(UUID.randomUUID(), "Late", "Player")
        playerService.create(latePlayer)

        val event = eventFactory.createFromRequest(
            OnePlayerEventRequest(
                type = EventType.PICKUP,
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

        assertIs<MatchCommandResult.Success<Match>>(
            matchService.startMatch(match.id, Instant.parse("2026-07-13T11:00:00Z")),
        )
        eventService.create(
            OnePlayerEvent(unknowns[0].participantId, Instant.parse("2026-07-13T11:59:58Z"), EventType.PULL),
            match.id,
        )
        eventService.create(
            OnePlayerEvent(unknowns[0].participantId, Instant.parse("2026-07-13T11:59:59Z"), EventType.PICKUP),
            match.id,
        )
        eventService.create(assertNotNull(event), match.id)
        val statistics = statisticsService.recalculateMatchStatistics(matchService.getOrThrow(match.id))

        assertEquals(
            1,
            statistics.playerStatistics.single { it.participantId == unknowns[0].participantId }.attack.passes,
        )
        assertEquals(
            1,
            statistics.playerStatistics.single { it.participantId == unknowns[1].participantId }.attack.catches,
        )
    }

    @Test
    fun `planned match update returns the re-read persisted match`() {
        val plannedStart = Instant.parse("2026-08-14T10:00:00Z")

        val result = matchService.update(match.id, null, plannedStart)

        val success = assertIs<MatchCommandResult.Success<Match>>(result)
        val persisted = matchService.getOrThrow(match.id)
        assertEquals(plannedStart, success.value.plannedStartTimestamp)
        assertEquals(success.value, persisted)
    }

    @Test
    fun `in-progress match with events rejects timestamp-only update`() {
        assertIs<MatchCommandResult.Success<Match>>(
            matchService.startMatch(match.id, Instant.parse("2026-08-14T09:00:00Z")),
        )
        eventService.create(
            TwoPlayerEvent(
                firstPlayer.id,
                secondPlayer.id,
                Instant.parse("2026-08-14T10:00:00Z"),
                EventType.PASS,
            ),
            match.id,
        )
        val plannedStart = Instant.parse("2026-08-14T08:00:00Z")

        val result = matchService.update(match.id, null, plannedStart)

        val rejection = assertIs<MatchCommandResult.InvalidState>(result)
        assertEquals(MatchProblemCode.MATCH_UPDATE_LOCKED, rejection.problem.code)
        assertNull(matchService.getOrThrow(match.id).plannedStartTimestamp)
    }

    @Test
    fun `update after match start is rejected without changing persisted match`() {
        val startedAt = Instant.parse("2026-08-14T10:00:00Z")
        assertIs<MatchCommandResult.Success<Match>>(matchService.startMatch(match.id, startedAt))
        val beforeUpdate = matchService.getOrThrow(match.id)

        val result = matchService.update(
            match.id,
            listOf(secondTeam.id, firstTeam.id),
            Instant.parse("2026-08-14T11:00:00Z"),
        )

        val rejection = assertIs<MatchCommandResult.InvalidState>(result)
        assertEquals(MatchProblemCode.MATCH_UPDATE_LOCKED, rejection.problem.code)
        assertEquals(beforeUpdate, matchService.getOrThrow(match.id))
    }

    @Test
    fun `start persists client timestamp once and rejects repeat start`() {
        val startedAt = Instant.parse("2026-08-14T10:00:00Z")

        val firstStart = matchService.startMatch(match.id, startedAt)
        val secondStart = matchService.startMatch(match.id, Instant.parse("2026-08-14T10:01:00Z"))

        val success = assertIs<MatchCommandResult.Success<Match>>(firstStart)
        assertEquals(startedAt, success.value.startedAt)
        val rejection = assertIs<MatchCommandResult.InvalidState>(secondStart)
        assertEquals(MatchProblemCode.MATCH_ALREADY_STARTED, rejection.problem.code)
        assertEquals(startedAt, matchService.getOrThrow(match.id).startedAt)
    }

    @Test
    fun `finish of planned match is rejected without persisting an end timestamp`() {
        val result = matchService.endMatch(match.id, Instant.parse("2026-08-14T10:00:00Z"))

        val rejection = assertIs<MatchCommandResult.InvalidState>(result)
        assertEquals(MatchProblemCode.MATCH_NOT_STARTED, rejection.problem.code)
        val persisted = matchService.getOrThrow(match.id)
        assertNull(persisted.startedAt)
        assertNull(persisted.endedAt)
    }

    @Test
    fun `finish before latest persisted event is rejected without persisting an end timestamp`() {
        val eventTimestamp = Instant.parse("2026-08-14T10:00:00Z")
        assertIs<MatchCommandResult.Success<Match>>(matchService.startMatch(match.id, eventTimestamp.minusSeconds(60)))
        matchEventFixture.recordCompletedPoint(match.id, eventTimestamp)

        val result = matchService.endMatch(match.id, eventTimestamp.minusSeconds(1))

        val rejection = assertIs<MatchCommandResult.Conflict>(result)
        assertEquals(MatchProblemCode.END_BEFORE_LAST_EVENT, rejection.problem.code)
        val persisted = matchService.getOrThrow(match.id)
        assertNull(persisted.endedAt)
        assertEquals(3, persisted.events.size)
        assertEquals(eventTimestamp, persisted.events.last().occurredAt)
    }

    @Test
    fun `finish at latest persisted event timestamp returns finished match`() {
        val eventTimestamp = Instant.parse("2026-08-14T10:00:00Z")
        assertIs<MatchCommandResult.Success<Match>>(matchService.startMatch(match.id, eventTimestamp.minusSeconds(60)))
        matchEventFixture.recordCompletedPoint(match.id, eventTimestamp)

        val result = matchService.endMatch(match.id, eventTimestamp)

        val success = assertIs<MatchCommandResult.Success<Match>>(result)
        assertEquals(eventTimestamp, success.value.endedAt)
        assertEquals(com.github.mihanizzm.ultistats.model.MatchStatus.FINISHED, success.value.status)
        assertEquals(success.value, matchService.getOrThrow(match.id))
    }

    @Test
    fun `missing match commands return not found and do not create a match`() {
        val missingMatchId = UUID.randomUUID()

        assertIs<MatchCommandResult.NotFound>(matchService.update(missingMatchId, null, Instant.now()))
        assertIs<MatchCommandResult.NotFound>(matchService.startMatch(missingMatchId, Instant.now()))
        assertIs<MatchCommandResult.NotFound>(matchService.endMatch(missingMatchId, Instant.now()))

        assertNull(matchService.get(missingMatchId))
    }
}
