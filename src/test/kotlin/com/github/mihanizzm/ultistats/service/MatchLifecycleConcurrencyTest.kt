package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.SystemEvent
import com.github.mihanizzm.ultistats.service.result.EventCommandResult
import com.github.mihanizzm.ultistats.service.result.MatchCommandResult
import com.github.mihanizzm.ultistats.validation.match.MatchProblemCode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Suppress("NonAsciiCharacters")
class MatchLifecycleConcurrencyTest {
    @Autowired
    lateinit var teamService: TeamService

    @Autowired
    lateinit var matchService: MatchService

    @Autowired
    lateinit var eventService: EventService

    @Test
    fun `одновременный старт матча сериализуется`() {
        val matchId = createPlannedMatch()
        val executor = Executors.newFixedThreadPool(2)
        val gate = CountDownLatch(1)

        try {
            val attempts = listOf(FIRST_START, SECOND_START).map { timestamp ->
                timestamp to executor.submit<MatchCommandResult<Match>> {
                    gate.await()
                    matchService.startMatch(matchId, timestamp)
                }
            }
            gate.countDown()
            val results = attempts.map { (timestamp, future) ->
                timestamp to future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }

            val (successfulTimestamp, success) = results
                .filter { it.second is MatchCommandResult.Success }
                .single()
            val rejection = assertIs<MatchCommandResult.InvalidState>(
                results.filterNot { it.second is MatchCommandResult.Success }.single().second,
            )
            val persisted = matchService.getOrThrow(matchId)

            assertEquals(MatchProblemCode.MATCH_ALREADY_STARTED, rejection.problem.code)
            assertEquals(successfulTimestamp, assertIs<MatchCommandResult.Success<Match>>(success).value.startedAt)
            assertEquals(successfulTimestamp, persisted.startedAt)
        } finally {
            gate.countDown()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(EXECUTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `одновременное завершение матча и создание события сериализуются`() {
        val matchId = createPlannedMatch()
        assertIs<MatchCommandResult.Success<Match>>(matchService.startMatch(matchId, MATCH_STARTED_AT))
        val executor = Executors.newFixedThreadPool(2)
        val gate = CountDownLatch(1)

        try {
            val finishFuture = executor.submit<MatchCommandResult<Match>> {
                gate.await()
                matchService.endMatch(matchId, FINISH_AT)
            }
            val eventFuture = executor.submit<EventCommandResult> {
                gate.await()
                eventService.create(SystemEvent(EVENT_AT, EventType.HALFTIME_START), matchId)
            }
            gate.countDown()

            val finishResult = finishFuture.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            val eventResult = eventFuture.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            val persisted = matchService.getOrThrow(matchId)

            when (finishResult) {
                is MatchCommandResult.Success -> {
                    val rejection = assertIs<EventCommandResult.InvalidState>(eventResult)
                    assertEquals(MatchProblemCode.MATCH_NOT_IN_PROGRESS, rejection.problem.code)
                    assertEquals(FINISH_AT, persisted.endedAt)
                    assertTrue(persisted.events.isEmpty())
                }

                is MatchCommandResult.Conflict -> {
                    assertEquals(MatchProblemCode.END_BEFORE_LAST_EVENT, finishResult.problem.code)
                    val success = assertIs<EventCommandResult.Success>(eventResult)
                    assertEquals(EVENT_AT, success.event.event.occurredAt)
                    assertNull(persisted.endedAt)
                    assertEquals(listOf(EVENT_AT), persisted.events.map { it.occurredAt })
                }

                else -> error("Unexpected finish result: $finishResult")
            }
            assertTrue(
                persisted.endedAt == null || persisted.events.none { it.occurredAt.isAfter(persisted.endedAt) },
                "A finished match must not contain an event after endedAt",
            )
        } finally {
            gate.countDown()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(EXECUTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        }
    }

    private fun createPlannedMatch(): UUID {
        val firstTeam = Team(UUID.randomUUID(), "Concurrency first")
        val secondTeam = Team(UUID.randomUUID(), "Concurrency second")
        val match = Match(UUID.randomUUID(), listOf(firstTeam.id, secondTeam.id))
        teamService.create(firstTeam)
        teamService.create(secondTeam)
        matchService.create(match)
        return match.id
    }

    private companion object {
        const val FUTURE_TIMEOUT_SECONDS = 10L
        const val EXECUTOR_TIMEOUT_SECONDS = 5L
        val FIRST_START: Instant = Instant.parse("2026-08-14T10:00:00Z")
        val SECOND_START: Instant = Instant.parse("2026-08-14T10:01:00Z")
        val MATCH_STARTED_AT: Instant = Instant.parse("2026-08-14T10:00:00Z")
        val FINISH_AT: Instant = Instant.parse("2026-08-14T10:01:00Z")
        val EVENT_AT: Instant = Instant.parse("2026-08-14T10:02:00Z")
    }
}
