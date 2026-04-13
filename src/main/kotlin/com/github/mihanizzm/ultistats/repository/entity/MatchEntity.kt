package com.github.mihanizzm.ultistats.repository.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "matches")
class MatchEntity(
    @Id
    val id: UUID,

    @Column(name = "team_ids", nullable = false, columnDefinition = "uuid[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    var teamIds: Array<UUID>,

    @Column(name = "events", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var eventsJson: String = "[]",

    @Column(name = "team_scores", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var teamScoresJson: String = "[]",

    @Column(name = "disk_holder_id")
    var diskHolderId: UUID? = null,

    @Column(name = "planned_start_timestamp")
    var plannedStartTimestamp: Instant? = null,

    @Column(name = "started_at")
    var startedAt: Instant? = null,

    @Column(name = "ended_at")
    var endedAt: Instant? = null,
)
