package com.github.mihanizzm.ultistats.controller

import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.dto.request.UpdateEventRequest
import com.github.mihanizzm.ultistats.dto.response.EventResponse
import com.github.mihanizzm.ultistats.facade.EventFacade
import com.github.mihanizzm.ultistats.facade.EventResult
import com.github.mihanizzm.ultistats.model.events.Event
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/matches/{matchId}/events")
@Tag(name = "Events", description = "Управление событиями матча")
class EventController(
    private val eventFacade: EventFacade,
) {
    @GetMapping
    @Operation(summary = "Получить все события матча")
    fun getAll(@PathVariable matchId: UUID): ResponseEntity<List<EventResponse>> =
        when (val result = eventFacade.getAll(matchId)) {
            is EventResult.EventList -> ResponseEntity.ok(result.events)
            else -> ResponseEntity.notFound().build()
        }

    @GetMapping("/{eventId}")
    @Operation(summary = "Получить событие по ID")
    fun get(
        @PathVariable matchId: UUID,
        @PathVariable eventId: UUID,
    ): ResponseEntity<EventResponse> = when (val result = eventFacade.get(matchId, eventId)) {
        is EventResult.Success -> ResponseEntity.ok(result.response)
        else -> ResponseEntity.notFound().build()
    }

    @PostMapping
    @Operation(summary = "Создать событие")
    fun create(
        @PathVariable matchId: UUID,
        @RequestBody request: CreateEventRequest
    ): ResponseEntity<EventResponse> =
        when (val result = eventFacade.create(matchId, request)) {
            is EventResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.response)
            is EventResult.NotFound -> ResponseEntity.notFound().build()
            is EventResult.BadRequest -> ResponseEntity.badRequest().build()
            else -> ResponseEntity.internalServerError().build()
        }

    @PatchMapping("/{eventId}")
    @Operation(summary = "Исправить участников события")
    fun update(
        @PathVariable matchId: UUID,
        @PathVariable eventId: UUID,
        @RequestBody request: UpdateEventRequest
    ): ResponseEntity<EventResponse> =
        when (val result = eventFacade.edit(matchId, eventId, request)) {
            is EventResult.Success -> ResponseEntity.ok(result.response)
            is EventResult.NotFound -> ResponseEntity.notFound().build()
            is EventResult.BadRequest -> ResponseEntity.badRequest().build()
            is EventResult.MethodNotAllowed -> ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build()
            else -> ResponseEntity.internalServerError().build()
        }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Удалить событие по ID")
    fun delete(
        @PathVariable matchId: UUID,
        @PathVariable eventId: UUID
    ): ResponseEntity<Unit> =
        when (eventFacade.delete(matchId, eventId)) {
            is EventResult.Deleted -> ResponseEntity.noContent().build()
            is EventResult.NotFound -> ResponseEntity.notFound().build()
            else -> ResponseEntity.internalServerError().build()
        }
}
