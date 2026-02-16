package com.github.mihanizzm.ultistats.controller

import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.dto.response.EventResponse
import com.github.mihanizzm.ultistats.model.events.*
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/matches/{matchId}/events")
@Tag(name = "Events", description = "Управление событиями матча")
class EventController(
    private val eventService: EventService,
    private val matchService: MatchService,
) {
    @GetMapping
    @Operation(summary = "Получить все события матча")
    fun getAll(@PathVariable matchId: UUID): ResponseEntity<List<Event>> {
        if (matchService.get(matchId) == null) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(eventService.getAllEventsOfMatch(matchId))
    }

    @PostMapping
    @Operation(summary = "Создать событие")
    fun create(
        @PathVariable matchId: UUID,
        @RequestBody request: CreateEventRequest
    ): ResponseEntity<EventResponse> {
        if (matchService.get(matchId) == null) {
            return ResponseEntity.notFound().build()
        }
        val event = createEventFromRequest(request)
            ?: return ResponseEntity.badRequest().build()
        val diskHolderId = eventService.create(event, matchId)
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse(diskHolderId))
    }

    @PutMapping("/{index}")
    @Operation(summary = "Изменить событие по индексу")
    fun edit(
        @PathVariable matchId: UUID,
        @PathVariable index: Int,
        @RequestBody request: CreateEventRequest
    ): ResponseEntity<EventResponse> {
        if (matchService.get(matchId) == null) {
            return ResponseEntity.notFound().build()
        }
        val event = createEventFromRequest(request)
            ?: return ResponseEntity.badRequest().build()
        return try {
            val diskHolderId = eventService.edit(index, event, matchId)
            ResponseEntity.ok(EventResponse(diskHolderId))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{index}")
    @Operation(summary = "Удалить событие по индексу")
    fun delete(
        @PathVariable matchId: UUID,
        @PathVariable index: Int
    ): ResponseEntity<EventResponse> {
        if (matchService.get(matchId) == null) {
            return ResponseEntity.notFound().build()
        }
        return try {
            val diskHolderId = eventService.remove(index, matchId)
            ResponseEntity.ok(EventResponse(diskHolderId))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    private fun createEventFromRequest(request: CreateEventRequest): Event? {
        return when (request.type) {
            EventType.PASS -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                PassEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.GOAL -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                GoalEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.DROP -> {
                if (request.playerId == null) return null
                DropEvent(
                    player = request.playerId,
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.PULL -> {
                if (request.playerId == null) return null
                PullEvent(
                    player = request.playerId,
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.BRICK -> {
                if (request.playerId == null) return null
                BrickEvent(
                    player = request.playerId,
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.TURNOVER -> {
                if (request.playerId == null) return null
                TurnoverEvent(
                    player = request.playerId,
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.BLOCK_MARKER -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                BlockMarkerEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.BLOCK_FIELD -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                BlockFieldEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.INTERCEPTION -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                InterceptionEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.CALLAHAN -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                CallahanEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.TIMEOUT_START -> {
                TimeoutStartEvent(
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.TIMEOUT_END -> {
                TimeoutEndEvent(
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.HALFTIME_START -> {
                HalftimeStartEvent(
                    realTimestamp = request.timestamp,
                )
            }
            EventType.HALFTIME_END -> {
                HalftimeEndEvent(
                    realTimestamp = request.timestamp,
                )
            }
        }
    }
}
