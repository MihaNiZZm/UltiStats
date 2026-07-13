package com.github.mihanizzm.ultistats.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.mihanizzm.ultistats.model.events.Event
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "matches")
/**
 * The persisted match row plus transient aggregate fields used by the existing API.
 * Team, roster, score, and event data live in normalized tables and are populated by
 * MatchServiceImpl when it builds a read model.
 */
data class Match(
    @Id
    val id: UUID,

    @Transient
    val teamIds: List<UUID>,

    @Transient
    val events: MutableList<Event> = mutableListOf(),

    @Transient
    val eventCount: Int = events.size,

    @Transient
    val teamScores: MutableList<TeamScore> = mutableListOf(),

    @Transient
    val playerIdsByTeam: Map<UUID, List<UUID>> = emptyMap(),

    @Column(name = "planned_start_timestamp")
    val plannedStartTimestamp: Instant? = null,

    @Column(name = "started_at")
    var startedAt: Instant? = null,

    @Column(name = "ended_at")
    var endedAt: Instant? = null,

    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
) {
    @get:JsonIgnore
    val status: MatchStatus
        get() = when {
            endedAt != null -> MatchStatus.FINISHED
            startedAt != null -> MatchStatus.IN_PROGRESS
            else -> MatchStatus.PLANNED
        }

}
