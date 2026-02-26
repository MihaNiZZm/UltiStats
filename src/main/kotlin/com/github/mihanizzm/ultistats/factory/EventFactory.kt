package com.github.mihanizzm.ultistats.factory

import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.events.EventCategory
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.SystemEvent
import com.github.mihanizzm.ultistats.model.events.TeamEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import org.springframework.stereotype.Component

@Component
class EventFactory {
    fun createFromRequest(request: CreateEventRequest): Event? {
        return when (request.type.category) {
            EventCategory.ONE_PLAYER -> {
                if (request.playerId == null) return null
                OnePlayerEvent(
                    player = request.playerId,
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                    type = request.type,
                )
            }
            EventCategory.TWO_PLAYER -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                TwoPlayerEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                    type = request.type,
                )
            }
            EventCategory.TEAM -> {
                TeamEvent(
                    team = request.teamId,
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
