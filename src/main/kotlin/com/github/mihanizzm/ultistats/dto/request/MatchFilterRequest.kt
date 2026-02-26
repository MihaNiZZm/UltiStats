package com.github.mihanizzm.ultistats.dto.request

import com.github.mihanizzm.ultistats.model.MatchStatus
import java.time.Instant
import java.util.UUID

data class MatchFilterRequest(
    val teamId: UUID? = null,
    val status: MatchStatus? = null,
    val dateFrom: Instant? = null,
    val dateTo: Instant? = null,
)
