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
import com.github.mihanizzm.ultistats.service.result.EventCommandResult
import com.github.mihanizzm.ultistats.validation.match.MatchProblem
import org.springframework.stereotype.Component
import java.util.UUID

sealed class EventResult {
    data class Success(val response: EventResponse) : EventResult()
    data class EventList(val events: List<EventResponse>) : EventResult()
    object Deleted : EventResult()
    object NotFound : EventResult()
    object BadRequest : EventResult()
    object MethodNotAllowed : EventResult()
    data class InvalidState(val problem: MatchProblem) : EventResult()
    data class Conflict(val problem: MatchProblem) : EventResult()
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
        return eventService.create(event, matchId).toFacadeResult()
    }

    fun edit(matchId: UUID, eventId: UUID, request: UpdateEventRequest): EventResult {
        if (matchService.get(matchId) == null) return EventResult.NotFound
        val stored = eventService.get(eventId, matchId) ?: return EventResult.NotFound
        if (request.type != stored.event.type) return EventResult.BadRequest
        val merged: CreateEventRequest = when {
            stored.event is OnePlayerEvent && request is OnePlayerEventPatchRequest ->
                OnePlayerEventRequest(stored.event.type, stored.event.occurredAt, request.participantId)
            stored.event is TwoPlayerEvent && request is TwoPlayerEventPatchRequest ->
                TwoPlayerEventRequest(
                    stored.event.type,
                    stored.event.occurredAt,
                    request.fromParticipantId ?: stored.event.fromParticipant,
                    request.toParticipantId ?: stored.event.toParticipant,
                )
            stored.event is TeamEvent && request is TeamEventPatchRequest ->
                TeamEventRequest(stored.event.type, stored.event.occurredAt, request.teamId)
            stored.event is SystemEvent -> return EventResult.MethodNotAllowed
            else -> return EventResult.BadRequest
        }
        val event = eventFactory.createFromRequest(merged, matchId) ?: return EventResult.BadRequest
        return eventService.update(eventId, event, matchId).toFacadeResult()
    }

    fun delete(matchId: UUID, eventId: UUID): EventResult {
        if (matchService.get(matchId) == null) return EventResult.NotFound
        return eventService.remove(eventId, matchId).toFacadeResult()
    }

    private fun EventCommandResult.toFacadeResult(): EventResult = when (this) {
        is EventCommandResult.Success -> EventResult.Success(EventResponse.from(event))
        EventCommandResult.Deleted -> EventResult.Deleted
        EventCommandResult.NotFound -> EventResult.NotFound
        is EventCommandResult.InvalidState -> EventResult.InvalidState(problem)
        is EventCommandResult.Conflict -> EventResult.Conflict(problem)
    }
}
