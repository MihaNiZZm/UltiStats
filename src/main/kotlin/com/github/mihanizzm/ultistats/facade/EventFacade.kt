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
        val event = eventFactory.createFromRequest(request, matchId)
            ?: return EventResult.BadRequest
        val eventId = eventService.create(event, matchId)
        return EventResult.Success(EventResponse(eventId))
    }

    fun edit(matchId: UUID, index: Int, request: UpdateEventRequest): EventResult {
        if (matchService.get(matchId) == null) {
            return EventResult.NotFound
        }
        val existingEvent = eventService.getAllEventsOfMatch(matchId).getOrNull(index)
            ?: return EventResult.NotFound

        val mergedRequest = when (existingEvent) {
            is com.github.mihanizzm.ultistats.model.events.OnePlayerEvent -> {
                CreateEventRequest(
                    type = request.type ?: existingEvent.type,
                    timestamp = request.timestamp ?: existingEvent.realTimestamp,
                    playerId = request.playerId ?: existingEvent.player,
                )
            }
            is com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent -> {
                CreateEventRequest(
                    type = request.type ?: existingEvent.type,
                    timestamp = request.timestamp ?: existingEvent.realTimestamp,
                    playerId = request.playerId ?: existingEvent.fromPlayer,
                    toPlayerId = request.toPlayerId ?: existingEvent.toPlayer,
                )
            }
            is com.github.mihanizzm.ultistats.model.events.TeamEvent -> {
                CreateEventRequest(
                    type = request.type ?: existingEvent.type,
                    timestamp = request.timestamp ?: existingEvent.realTimestamp,
                    teamId = request.teamId ?: existingEvent.team,
                )
            }
            is com.github.mihanizzm.ultistats.model.events.SystemEvent -> {
                CreateEventRequest(
                    type = request.type ?: existingEvent.type,
                    timestamp = request.timestamp ?: existingEvent.realTimestamp,
                )
            }
        }
        val event = eventFactory.createFromRequest(mergedRequest, matchId) ?: return EventResult.BadRequest

        return try {
            val eventId = eventService.edit(index, event, matchId)
            EventResult.Success(EventResponse(eventId))
        } catch (e: IllegalArgumentException) {
            EventResult.NotFound
        }
    }

    fun delete(matchId: UUID, index: Int): EventResult {
        if (matchService.get(matchId) == null) {
            return EventResult.NotFound
        }
        return try {
            val eventId = eventService.remove(index, matchId)
            EventResult.Success(EventResponse(eventId))
        } catch (e: IllegalArgumentException) {
            EventResult.NotFound
        }
    }
}
