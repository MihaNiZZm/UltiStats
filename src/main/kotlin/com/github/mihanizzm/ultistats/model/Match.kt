package com.github.mihanizzm.ultistats.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import java.time.Instant
import java.util.UUID

data class Match(
    val id: UUID,
    val teamIds: List<UUID>,
    val events: MutableList<Event> = mutableListOf(),
    val teamScores: MutableList<TeamScore> = mutableListOf(),
    var diskHolderId: UUID? = null,
    val plannedStartTimestamp: Instant? = null,
    var startedAt: Instant? = null,
    var endedAt: Instant? = null,
) {
    @get:JsonIgnore
    val status: MatchStatus
        get() = when {
            endedAt != null -> MatchStatus.FINISHED
            startedAt != null -> MatchStatus.IN_PROGRESS
            else -> MatchStatus.PLANNED
        }

    /**
     * Инициализировать счёт для команд.
     * Вызывается при создании матча.
     */
    fun initTeamScores() {
        teamScores.clear()
        teamIds.forEach { teamId ->
            teamScores.add(TeamScore(teamId, 0))
        }
    }

    /**
     * Пересчитать счёт команд по событиям.
     * Вызывается при добавлении/изменении/удалении событий.
     */
    fun recalculateTeamScores() {
        // Сбрасываем счёт
        teamScores.forEach { it.score = 0 }

        // Подсчитываем очки за GOAL и CALLAHAN
        events.forEach { event ->
            if (event.type == EventType.GOAL || event.type == EventType.CALLAHAN) {
                val scoringTeamId = (event as TwoPlayerEvent).toTeam
                val teamScore = teamScores.find { it.teamId == scoringTeamId }
                if (teamScore == null) {
                    throw RuntimeException("Team with this id can't be found")
                }
                teamScore?.score = (teamScore?.score ?: 0) + 1
            }
        }
    }
}
