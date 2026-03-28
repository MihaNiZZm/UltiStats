package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.dto.request.UpdateEventRequest
import com.github.mihanizzm.ultistats.dto.response.EventResponse
import com.github.mihanizzm.ultistats.factory.EventFactory
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import org.springframework.stereotype.Component
import java.util.UUID

sealed class EventResult {
    data class Success(val response: EventResponse) : EventResult()
    data class EventList(val events: List<Event>) : EventResult()
    object NotFound : EventResult()
    object BadRequest : EventResult()
}

@Component
class EventFacade(
    private val eventService: EventService,
    private val matchService: MatchService,
    private val eventFactory: EventFactory,
) {
    fun getAll(matchId: UUID): EventResult {
        if (matchService.get(matchId) == null) {
            return EventResult.NotFound
        }
        return EventResult.EventList(eventService.getAllEventsOfMatch(matchId))
    }

    fun create(matchId: UUID, request: CreateEventRequest): EventResult {
        if (matchService.get(matchId) == null) {
            return EventResult.NotFound
        }
        val event = eventFactory.createFromRequest(request)
            ?: return EventResult.BadRequest
        val diskHolderId = eventService.create(event, matchId)
        return EventResult.Success(EventResponse(diskHolderId))
    }

    fun edit(matchId: UUID, index: Int, request: UpdateEventRequest): EventResult {
        if (matchService.get(matchId) == null) {
            return EventResult.NotFound
        }
        val existingEvent = eventService.getAllEventsOfMatch(matchId).getOrNull(index)
            ?: return EventResult.NotFound

        // Частичное обновление: используем существующие значения, если новые не переданы
        val event = when (existingEvent) {
            is com.github.mihanizzm.ultistats.model.events.OnePlayerEvent -> {
                existingEvent.copy(
                    type = request.type ?: existingEvent.type,
                    realTimestamp = request.timestamp ?: existingEvent.realTimestamp,
                    team = request.teamId ?: existingEvent.team,
                    player = request.playerId ?: existingEvent.player,
                )
            }
            is com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent -> {
                existingEvent.copy(
                    type = request.type ?: existingEvent.type,
                    realTimestamp = request.timestamp ?: existingEvent.realTimestamp,
                    fromTeam = request.teamId ?: existingEvent.fromTeam,
                    toTeam = request.toTeamId ?: existingEvent.toTeam,
                    fromPlayer = request.playerId ?: existingEvent.fromPlayer,
                    toPlayer = request.toPlayerId ?: existingEvent.toPlayer,
                )
            }
            is com.github.mihanizzm.ultistats.model.events.TeamEvent -> {
                existingEvent.copy(
                    type = request.type ?: existingEvent.type,
                    realTimestamp = request.timestamp ?: existingEvent.realTimestamp,
                    team = request.teamId ?: existingEvent.team,
                )
            }
            is com.github.mihanizzm.ultistats.model.events.SystemEvent -> {
                existingEvent.copy(
                    type = request.type ?: existingEvent.type,
                    realTimestamp = request.timestamp ?: existingEvent.realTimestamp,
                )
            }
        }

        return try {
            val diskHolderId = eventService.edit(index, event, matchId)
            EventResult.Success(EventResponse(diskHolderId))
        } catch (e: IllegalArgumentException) {
            EventResult.NotFound
        }
    }

    fun delete(matchId: UUID, index: Int): EventResult {
        if (matchService.get(matchId) == null) {
            return EventResult.NotFound
        }
        return try {
            val diskHolderId = eventService.remove(index, matchId)
            EventResult.Success(EventResponse(diskHolderId))
        } catch (e: IllegalArgumentException) {
            EventResult.NotFound
        }
    }
}
