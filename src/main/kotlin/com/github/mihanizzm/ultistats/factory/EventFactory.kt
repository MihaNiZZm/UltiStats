package com.github.mihanizzm.ultistats.factory

import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.events.EventCategory
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.SystemEvent
import com.github.mihanizzm.ultistats.model.events.TeamEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataMatchPlayerRepository
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataMatchTeamRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EventFactory(
    private val matchPlayerRepository: SpringDataMatchPlayerRepository,
    private val matchTeamRepository: SpringDataMatchTeamRepository,
) {
    fun createFromRequest(request: CreateEventRequest, matchId: UUID): Event? {
        val teamByPlayerId = matchPlayerRepository.findAllByMatchId(matchId)
            .associate { it.playerId to it.teamId }
        return when (request.type.category) {
            EventCategory.ONE_PLAYER -> {
                val playerId = request.playerId ?: return null
                if (playerId !in teamByPlayerId) return null
                OnePlayerEvent(
                    player = playerId,
                    realTimestamp = request.timestamp,
                    type = request.type,
                )
            }
            EventCategory.TWO_PLAYER -> {
                val playerId = request.playerId ?: return null
                val toPlayerId = request.toPlayerId ?: return null
                if (playerId !in teamByPlayerId || toPlayerId !in teamByPlayerId) return null
                TwoPlayerEvent(
                    fromPlayer = playerId,
                    toPlayer = toPlayerId,
                    realTimestamp = request.timestamp,
                    type = request.type,
                )
            }
            EventCategory.TEAM -> {
                val teamId = request.teamId ?: return null
                if (matchTeamRepository.findAllByMatchIdOrderByPosition(matchId).none { it.teamId == teamId }) return null
                TeamEvent(
                    team = teamId,
                    realTimestamp = request.timestamp,
                    type = request.type,
                )
            }
            EventCategory.SYSTEM -> {
                SystemEvent(
                    realTimestamp = request.timestamp,
                    type = request.type,
                )
            }
        }
    }
}
