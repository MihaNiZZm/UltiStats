package com.github.mihanizzm.ultistats.validation.match

import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.MatchStatus
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.SystemEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.util.UUID
import java.util.stream.Stream

@Suppress("NonAsciiCharacters")
class MatchLifecyclePolicyTest {
    private val policy = MatchLifecyclePolicy()

    @ParameterizedTest
    @MethodSource("updateStates")
    fun `изменение матча разрешено только до начала`(match: Match, expected: ExpectedDecision) {
        assertDecision(policy.validateUpdate(match), expected)
    }

    @ParameterizedTest
    @MethodSource("startStates")
    fun `старт матча разрешен только для запланированного матча`(match: Match, expected: ExpectedDecision) {
        assertDecision(policy.validateStart(match, EVENT_AT), expected)
    }

    @ParameterizedTest
    @MethodSource("finishStates")
    fun `завершение матча разрешено только для идущего матча`(match: Match, expected: ExpectedDecision) {
        assertDecision(policy.validateFinish(match, END_AT), expected)
    }

    @ParameterizedTest
    @MethodSource("eventCreationStates")
    fun `создание события разрешено только во время матча`(match: Match, expected: ExpectedDecision) {
        assertDecision(policy.validateEventCreation(match, EVENT_AT), expected)
    }

    @ParameterizedTest
    @MethodSource("eventModificationStates")
    fun `исправление события разрешено после старта матча`(match: Match, expected: ExpectedDecision) {
        assertDecision(policy.validateEventUpdate(match), expected)
    }

    @ParameterizedTest
    @MethodSource("eventModificationStates")
    fun `удаление события разрешено после старта матча`(match: Match, expected: ExpectedDecision) {
        assertDecision(policy.validateEventDeletion(match), expected)
    }

    @Test
    fun `старт не может быть позже первого события но равенство допустимо`() {
        val match = match(MatchStatus.PLANNED).withEvent(EVENT_AT)

        assertDecision(policy.validateStart(match, EVENT_AT.minusSeconds(1)), allowed())
        assertDecision(policy.validateStart(match, EVENT_AT), allowed())
        assertDecision(policy.validateStart(match, EVENT_AT.plusSeconds(1)), conflict(MatchProblemCode.START_AFTER_FIRST_EVENT))
    }

    @Test
    fun `завершение не может предшествовать старту или последнему событию но равенство допустимо`() {
        val matchWithEvent = match(MatchStatus.IN_PROGRESS).withEvent(EVENT_AT)

        assertDecision(policy.validateFinish(match(MatchStatus.IN_PROGRESS), START_AT), allowed())
        assertDecision(policy.validateFinish(matchWithEvent, START_AT.minusSeconds(1)), conflict(MatchProblemCode.END_BEFORE_START))
        assertDecision(policy.validateFinish(matchWithEvent, EVENT_AT.minusSeconds(1)), conflict(MatchProblemCode.END_BEFORE_LAST_EVENT))
        assertDecision(policy.validateFinish(matchWithEvent, EVENT_AT), allowed())
    }

    @Test
    fun `новое событие не может предшествовать старту или последнему событию но равенство допустимо`() {
        val matchWithEvent = match(MatchStatus.IN_PROGRESS).withEvent(EVENT_AT)

        assertDecision(policy.validateEventCreation(match(MatchStatus.IN_PROGRESS), START_AT), allowed())
        assertDecision(policy.validateEventCreation(matchWithEvent, START_AT.minusSeconds(1)), conflict(MatchProblemCode.EVENT_BEFORE_START))
        assertDecision(policy.validateEventCreation(matchWithEvent, EVENT_AT.minusSeconds(1)), conflict(MatchProblemCode.EVENT_OUT_OF_ORDER))
        assertDecision(policy.validateEventCreation(matchWithEvent, EVENT_AT), allowed())
    }

    @Test
    fun `плановое время и текущее время не влияют на решение`() {
        val matchWithFuturePlan = match(
            status = MatchStatus.PLANNED,
            plannedStartTimestamp = Instant.parse("2099-01-01T00:00:00Z"),
        )
        val matchWithPastPlan = match(
            status = MatchStatus.PLANNED,
            plannedStartTimestamp = Instant.parse("2000-01-01T00:00:00Z"),
        )

        assertDecision(policy.validateStart(matchWithFuturePlan, Instant.parse("2001-01-01T00:00:00Z")), allowed())
        assertDecision(policy.validateStart(matchWithPastPlan, Instant.parse("2098-01-01T00:00:00Z")), allowed())
    }

    private fun assertDecision(actual: MatchLifecycleDecision, expected: ExpectedDecision) {
        assertThat(actual).isInstanceOf(expected.type)
        when (actual) {
            MatchLifecycleDecision.Allowed -> assertThat(expected.code).isNull()
            is MatchLifecycleDecision.InvalidState -> assertThat(actual.problem.code).isEqualTo(expected.code)
            is MatchLifecycleDecision.Conflict -> assertThat(actual.problem.code).isEqualTo(expected.code)
        }
    }

    private fun match(
        status: MatchStatus,
        plannedStartTimestamp: Instant? = null,
    ): Match = Match(
        id = UUID.randomUUID(),
        teamIds = emptyList(),
        plannedStartTimestamp = plannedStartTimestamp,
        startedAt = if (status == MatchStatus.PLANNED) null else START_AT,
        endedAt = if (status == MatchStatus.FINISHED) END_AT else null,
    )

    private fun Match.withEvent(occurredAt: Instant): Match = apply {
        events += SystemEvent(occurredAt, EventType.HALFTIME_START)
    }

    data class ExpectedDecision(
        val type: Class<out MatchLifecycleDecision>,
        val code: MatchProblemCode? = null,
    )

    companion object {
        private val START_AT: Instant = Instant.parse("2026-08-14T10:00:00Z")
        private val EVENT_AT: Instant = Instant.parse("2026-08-14T10:10:00Z")
        private val END_AT: Instant = Instant.parse("2026-08-14T10:20:00Z")

        @JvmStatic
        fun updateStates(): Stream<Arguments> = Stream.of(
            Arguments.of(matchFor(MatchStatus.PLANNED), allowed()),
            Arguments.of(matchFor(MatchStatus.IN_PROGRESS), invalidState(MatchProblemCode.MATCH_UPDATE_LOCKED)),
            Arguments.of(matchFor(MatchStatus.FINISHED), invalidState(MatchProblemCode.MATCH_UPDATE_LOCKED)),
        )

        @JvmStatic
        fun startStates(): Stream<Arguments> = Stream.of(
            Arguments.of(matchFor(MatchStatus.PLANNED), allowed()),
            Arguments.of(matchFor(MatchStatus.IN_PROGRESS), invalidState(MatchProblemCode.MATCH_ALREADY_STARTED)),
            Arguments.of(matchFor(MatchStatus.FINISHED), invalidState(MatchProblemCode.MATCH_ALREADY_FINISHED)),
        )

        @JvmStatic
        fun finishStates(): Stream<Arguments> = Stream.of(
            Arguments.of(matchFor(MatchStatus.PLANNED), invalidState(MatchProblemCode.MATCH_NOT_STARTED)),
            Arguments.of(matchFor(MatchStatus.IN_PROGRESS), allowed()),
            Arguments.of(matchFor(MatchStatus.FINISHED), invalidState(MatchProblemCode.MATCH_ALREADY_FINISHED)),
        )

        @JvmStatic
        fun eventCreationStates(): Stream<Arguments> = Stream.of(
            Arguments.of(matchFor(MatchStatus.PLANNED), invalidState(MatchProblemCode.MATCH_NOT_IN_PROGRESS)),
            Arguments.of(matchFor(MatchStatus.IN_PROGRESS), allowed()),
            Arguments.of(matchFor(MatchStatus.FINISHED), invalidState(MatchProblemCode.MATCH_NOT_IN_PROGRESS)),
        )

        @JvmStatic
        fun eventModificationStates(): Stream<Arguments> = Stream.of(
            Arguments.of(matchFor(MatchStatus.PLANNED), invalidState(MatchProblemCode.MATCH_NOT_IN_PROGRESS)),
            Arguments.of(matchFor(MatchStatus.IN_PROGRESS), allowed()),
            Arguments.of(matchFor(MatchStatus.FINISHED), allowed()),
        )

        private fun matchFor(status: MatchStatus): Match = Match(
            id = UUID.randomUUID(),
            teamIds = emptyList(),
            startedAt = if (status == MatchStatus.PLANNED) null else START_AT,
            endedAt = if (status == MatchStatus.FINISHED) END_AT else null,
        )

        private fun allowed() = ExpectedDecision(MatchLifecycleDecision.Allowed::class.java)

        private fun invalidState(code: MatchProblemCode) = ExpectedDecision(MatchLifecycleDecision.InvalidState::class.java, code)

        private fun conflict(code: MatchProblemCode) = ExpectedDecision(MatchLifecycleDecision.Conflict::class.java, code)
    }
}
