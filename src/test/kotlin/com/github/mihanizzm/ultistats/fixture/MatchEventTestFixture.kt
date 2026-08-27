package com.github.mihanizzm.ultistats.fixture

import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.result.EventCommandResult
import java.time.Instant
import java.util.UUID
import kotlin.test.assertIs

class MatchEventTestFixture(
    private val matchService: MatchService,
    private val eventService: EventService,
) {
    fun recordCompletedPoint(matchId: UUID, goalAt: Instant) {
        val teammates = matchService.getOrThrow(matchId).participantsByTeam.values
            .first { it.size >= 2 }
        val first = teammates[0].participantId
        val second = teammates[1].participantId

        assertIs<EventCommandResult.Success>(
            eventService.create(OnePlayerEvent(first, goalAt.minusSeconds(2), EventType.PULL), matchId),
        )
        assertIs<EventCommandResult.Success>(
            eventService.create(OnePlayerEvent(second, goalAt.minusSeconds(1), EventType.PICKUP), matchId),
        )
        assertIs<EventCommandResult.Success>(
            eventService.create(TwoPlayerEvent(first, second, goalAt, EventType.GOAL), matchId),
        )
    }
}
