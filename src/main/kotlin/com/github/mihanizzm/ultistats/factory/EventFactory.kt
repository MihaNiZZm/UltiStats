package com.github.mihanizzm.ultistats.factory

import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.dto.request.OnePlayerEventRequest
import com.github.mihanizzm.ultistats.dto.request.SystemEventRequest
import com.github.mihanizzm.ultistats.dto.request.TeamEventRequest
import com.github.mihanizzm.ultistats.dto.request.TwoPlayerEventRequest
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.events.EventCategory
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.SystemEvent
import com.github.mihanizzm.ultistats.model.events.TeamEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataMatchParticipantRepository
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataMatchTeamRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EventFactory(
    private val matchParticipantRepository: SpringDataMatchParticipantRepository,
    private val matchTeamRepository: SpringDataMatchTeamRepository,
) {
    fun createFromRequest(request: CreateEventRequest, matchId: UUID): Event? {
        val teamByParticipantId = matchParticipantRepository.findAllByMatchId(matchId)
            .associate { it.participantId to it.teamId }
        return when (request) {
            is OnePlayerEventRequest -> {
                if (
                    request.type.category != EventCategory.ONE_PLAYER ||
                    request.participantId !in teamByParticipantId
                ) return null
                OnePlayerEvent(request.participantId, request.occurredAt, request.type)
            }
            is TwoPlayerEventRequest -> {
                if (request.type.category != EventCategory.TWO_PLAYER) return null
                if (request.fromParticipantId == request.toParticipantId) return null
                val fromTeam = teamByParticipantId[request.fromParticipantId] ?: return null
                val toTeam = teamByParticipantId[request.toParticipantId] ?: return null
                val sameTeamRequired = request.type == EventType.PASS || request.type == EventType.GOAL
                if (sameTeamRequired != (fromTeam == toTeam)) return null
                TwoPlayerEvent(
                    request.fromParticipantId,
                    request.toParticipantId,
                    request.occurredAt,
                    request.type,
                )
            }
            is TeamEventRequest -> {
                if (request.type.category != EventCategory.TEAM) return null
                val teamExists = matchTeamRepository.findAllByMatchIdOrderByPosition(matchId)
                    .any { it.teamId == request.teamId }
                if (!teamExists) return null
                TeamEvent(request.teamId, request.occurredAt, request.type)
            }
            is SystemEventRequest -> {
                if (request.type.category != EventCategory.SYSTEM) return null
                SystemEvent(request.occurredAt, request.type)
            }
        }
    }
}
