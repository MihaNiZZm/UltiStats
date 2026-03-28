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
    fun getAll(@PathVariable matchId: UUID): ResponseEntity<List<Event>> =
        when (val result = eventFacade.getAll(matchId)) {
            is EventResult.EventList -> ResponseEntity.ok(result.events)
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

    @PutMapping("/{index}")
    @Operation(summary = "Изменить событие по индексу (частичное обновление)")
    fun update(
        @PathVariable matchId: UUID,
        @PathVariable index: Int,
        @RequestBody request: UpdateEventRequest
    ): ResponseEntity<EventResponse> =
        when (val result = eventFacade.edit(matchId, index, request)) {
            is EventResult.Success -> ResponseEntity.ok(result.response)
            is EventResult.NotFound -> ResponseEntity.notFound().build()
            is EventResult.BadRequest -> ResponseEntity.badRequest().build()
            else -> ResponseEntity.internalServerError().build()
        }

    @DeleteMapping("/{index}")
    @Operation(summary = "Удалить событие по индексу")
    fun delete(
        @PathVariable matchId: UUID,
        @PathVariable index: Int
    ): ResponseEntity<EventResponse> =
        when (val result = eventFacade.delete(matchId, index)) {
            is EventResult.Success -> ResponseEntity.ok(result.response)
            is EventResult.NotFound -> ResponseEntity.notFound().build()
            else -> ResponseEntity.internalServerError().build()
        }
}
