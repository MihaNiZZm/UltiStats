package com.github.mihanizzm.ultistats.facade

import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.dto.request.OnePlayerEventPatchRequest
import com.github.mihanizzm.ultistats.dto.request.OnePlayerEventRequest
import com.github.mihanizzm.ultistats.dto.request.TeamEventPatchRequest
import com.github.mihanizzm.ultistats.dto.request.TeamEventRequest
import com.github.mihanizzm.ultistats.dto.request.TwoPlayerEventPatchRequest
import com.github.mihanizzm.ultistats.dto.request.TwoPlayerEventRequest
import com.github.mihanizzm.ultistats.dto.request.UpdateEventRequest
import com.github.mihanizzm.ultistats.dto.response.EventResponse
import com.github.mihanizzm.ultistats.factory.EventFactory
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.SystemEvent
import com.github.mihanizzm.ultistats.model.events.TeamEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import org.springframework.stereotype.Component
import java.util.UUID

sealed class EventResult {
    data class Success(val response: EventResponse) : EventResult()
    data class EventList(val events: List<EventResponse>) : EventResult()
    object Deleted : EventResult()
    object NotFound : EventResult()
    object BadRequest : EventResult()
    object MethodNotAllowed : EventResult()
}

@Component
class EventFacade(
    private val eventService: EventService,
    private val matchService: MatchService,
    private val eventFactory: EventFactory,
) {
    fun getAll(matchId: UUID): EventResult {
        if (matchService.get(matchId) == null) return EventResult.NotFound
        return EventResult.EventList(eventService.getAllEventsOfMatch(matchId).map(EventResponse::from))
    }

    fun get(matchId: UUID, eventId: UUID): EventResult {
        if (matchService.get(matchId) == null) return EventResult.NotFound
        return eventService.get(eventId, matchId)?.let { EventResult.Success(EventResponse.from(it)) }
            ?: EventResult.NotFound
    }

    fun create(matchId: UUID, request: CreateEventRequest): EventResult {
        if (matchService.get(matchId) == null) return EventResult.NotFound
        val event = eventFactory.createFromRequest(request, matchId) ?: return EventResult.BadRequest
        return try {
            EventResult.Success(EventResponse.from(eventService.create(event, matchId)))
        } catch (_: IllegalArgumentException) {
            EventResult.BadRequest
        }
    }

    fun edit(matchId: UUID, eventId: UUID, request: UpdateEventRequest): EventResult {
        if (matchService.get(matchId) == null) return EventResult.NotFound
        val stored = eventService.get(eventId, matchId) ?: return EventResult.NotFound
        if (request.type != stored.event.type) return EventResult.BadRequest
        val merged: CreateEventRequest = when {
            stored.event is OnePlayerEvent && request is OnePlayerEventPatchRequest ->
                OnePlayerEventRequest(stored.event.type, stored.event.occurredAt, request.playerId)
            stored.event is TwoPlayerEvent && request is TwoPlayerEventPatchRequest ->
                TwoPlayerEventRequest(
                    stored.event.type,
                    stored.event.occurredAt,
                    request.fromPlayerId ?: stored.event.fromPlayer,
                    request.toPlayerId ?: stored.event.toPlayer,
                )
            stored.event is TeamEvent && request is TeamEventPatchRequest ->
                TeamEventRequest(stored.event.type, stored.event.occurredAt, request.teamId)
            stored.event is SystemEvent -> return EventResult.MethodNotAllowed
            else -> return EventResult.BadRequest
        }
        val event = eventFactory.createFromRequest(merged, matchId) ?: return EventResult.BadRequest
        return EventResult.Success(EventResponse.from(eventService.update(eventId, event, matchId)))
    }

    fun delete(matchId: UUID, eventId: UUID): EventResult {
        if (matchService.get(matchId) == null) return EventResult.NotFound
        return if (eventService.remove(eventId, matchId)) EventResult.Deleted else EventResult.NotFound
    }
}
