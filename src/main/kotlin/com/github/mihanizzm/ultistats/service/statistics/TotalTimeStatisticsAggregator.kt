package com.github.mihanizzm.ultistats.service.statistics

import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.TeamEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import com.github.mihanizzm.ultistats.model.statistics.MatchTimeStatistics
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Component
@Suppress("unused")
class TotalTimeStatisticsAggregator : StatisticsAggregator {
    override fun aggregate(previousStatisticsState: MatchStatistics, events: List<Event>): MatchStatistics {
        var lastEventTime: Instant? = null
        var currentTeamPossessing: UUID? = null
        var currentPlayerPossessing: UUID? = null
        var isStopped = false
        var teamOnTimeout: UUID? = null
        var lastBetweenPointsStart: Instant? = null

        val teamPossessionTime = mutableMapOf<UUID, Duration>().withDefault { Duration.ZERO }
        val teamTimeBetweenPoints = mutableMapOf<UUID, Duration>().withDefault { Duration.ZERO }
        val playerPossessionTime = mutableMapOf<UUID, Duration>().withDefault { Duration.ZERO }
        val timeSpentOnTimeouts = mutableMapOf<UUID, Duration>().withDefault { Duration.ZERO }
        var totalMatchTime = Duration.ZERO
        var timeSpentOnHalftime = Duration.ZERO

        for (event in events) {
            var duration = Duration.ZERO
            if (lastEventTime != null) {
                duration = Duration.between(lastEventTime, event.realTimestamp)
                totalMatchTime = totalMatchTime.plus(duration)

                if (currentTeamPossessing != null && !isStopped) {
                    teamPossessionTime.merge(currentTeamPossessing, duration, Duration::plus)
                }
                if (currentPlayerPossessing != null && !isStopped) {
                    playerPossessionTime.merge(currentPlayerPossessing, duration, Duration::plus)
                }
            }

            lastEventTime = event.realTimestamp

            when (event.type) {
                EventType.PASS -> {
                    currentPlayerPossessing = (event as TwoPlayerEvent).toPlayer
                }
                EventType.GOAL -> {
                    currentPlayerPossessing = null
                    currentTeamPossessing = null
                    isStopped = true
                    // Начинаем отсчет времени между поинтами от момента гола
                    lastBetweenPointsStart = event.realTimestamp
                }
                EventType.PULL -> {
                    val pullTeam = (event as OnePlayerEvent).team
                    // Добавляем во время между поинтами только чистый отрезок между окончанием предыдущего поинта
                    // и текущим пуллом, исключая таймауты и халфтайм.
                    val between = if (lastBetweenPointsStart != null) {
                        Duration.between(lastBetweenPointsStart, event.realTimestamp)
                    } else {
                        duration
                    }
                    if (!between.isNegative && !between.isZero) {
                        teamTimeBetweenPoints.merge(pullTeam, between, Duration::plus)
                    }
                    // После пулла отсчет между поинтами завершен
                    lastBetweenPointsStart = null
                    isStopped = false
                }
                EventType.BRICK -> { }
                EventType.TURNOVER -> {
                    val ope = event as OnePlayerEvent
                    currentTeamPossessing = ope.team
                    currentPlayerPossessing = ope.player
                }
                EventType.BLOCK_MARKER, EventType.BLOCK_FIELD, EventType.DROP -> {
                    currentTeamPossessing = null
                    currentPlayerPossessing = null
                }
                EventType.INTERCEPTION -> {
                    val tpe = event as TwoPlayerEvent
                    currentTeamPossessing = tpe.toTeam
                    currentPlayerPossessing = tpe.toPlayer
                }
                EventType.CALLAHAN -> {
                    currentTeamPossessing = null
                    currentPlayerPossessing = null
                    isStopped = true
                    // Кэллахан завершает поинт
                    lastBetweenPointsStart = event.realTimestamp
                }
                EventType.TIMEOUT_START -> {
                    teamOnTimeout = (event as TeamEvent).team
                    isStopped = true
                }
                EventType.TIMEOUT_END -> {
                    isStopped = false
                    if (teamOnTimeout != null) {
                        timeSpentOnTimeouts.merge(teamOnTimeout, duration, Duration::plus)
                        teamOnTimeout = null
                    }
                    // Между поинтами не должно включать время таймаута, поэтому старт переносим на конец таймаута
                    if (lastBetweenPointsStart != null) {
                        lastBetweenPointsStart = event.realTimestamp
                    }
                }
                EventType.HALFTIME_START -> {
                    currentPlayerPossessing = null
                    currentTeamPossessing = null
                    isStopped = true
                }
                EventType.HALFTIME_END -> {
                    isStopped = false
                    timeSpentOnHalftime = timeSpentOnHalftime.plus(duration)
                    // Между поинтами не должно включать время халфтайма, поэтому старт переносим на конец халфтайма
                    if (lastBetweenPointsStart != null) {
                        lastBetweenPointsStart = event.realTimestamp
                    }
                }
            }
        }

        val totalTimeBetweenPoints = teamTimeBetweenPoints.values.fold(Duration.ZERO, Duration::plus)
        val totalTimeOnTimeouts = timeSpentOnTimeouts.values.fold(Duration.ZERO, Duration::plus)
        val purePlayTime = totalMatchTime
            .minus(totalTimeBetweenPoints)
            .minus(totalTimeOnTimeouts)
            .minus(timeSpentOnHalftime)

        val newPlayerStats = previousStatisticsState.playerStatistics.map { stats ->
            val possession = playerPossessionTime.getOrDefault(stats.playerId, Duration.ZERO)
            stats.copy(time = stats.time.copy(totalPossessionTime = possession))
        }.toMutableList()

        val newTeamStats = previousStatisticsState.teamStatistics.map { stats ->
            stats.copy(
                time = stats.time.copy(
                    totalPossessionTime = teamPossessionTime.getOrDefault(stats.teamId, Duration.ZERO),
                    totalTimeBetweenPoints = teamTimeBetweenPoints.getOrDefault(stats.teamId, Duration.ZERO),
                    totalTimeSpentOnTimeouts = timeSpentOnTimeouts.getOrDefault(stats.teamId, Duration.ZERO),
                )
            )
        }.toMutableList()

        return previousStatisticsState.copy(
            playerStatistics = newPlayerStats,
            teamStatistics = newTeamStats,
            timeStatistics = MatchTimeStatistics(
                totalTime = totalMatchTime,
                timeSpentBetweenPoints = totalTimeBetweenPoints,
                timeSpentOnTimeouts = totalTimeOnTimeouts,
                timeSpentOnHalftime = timeSpentOnHalftime,
                pureGameTime = purePlayTime,
            ),
        )
    }
}