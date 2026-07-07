package com.github.mihanizzm.ultistats.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.repository.converter.EventListJsonConverter
import com.github.mihanizzm.ultistats.repository.converter.TeamScoreListJsonConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "matches")
data class Match(
    @Id
    val id: UUID,

    @Column(name = "team_ids", nullable = false, columnDefinition = "uuid[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    val teamIds: List<UUID>,

    @Column(name = "events", columnDefinition = "jsonb")
    @Convert(converter = EventListJsonConverter::class)
    val events: MutableList<Event> = mutableListOf(),

    @Column(name = "team_scores", columnDefinition = "jsonb")
    @Convert(converter = TeamScoreListJsonConverter::class)
    val teamScores: MutableList<TeamScore> = mutableListOf(),

    @Column(name = "disk_holder_id")
    var diskHolderId: UUID? = null,

    @Column(name = "planned_start_timestamp")
    val plannedStartTimestamp: Instant? = null,

    @Column(name = "started_at")
    var startedAt: Instant? = null,

    @Column(name = "ended_at")
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
                teamScore.score += 1
            }
        }
    }
}
