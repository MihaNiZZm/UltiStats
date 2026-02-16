package com.github.mihanizzm.ultistats.controller

import com.github.mihanizzm.ultistats.dto.request.CreateMatchRequest
import com.github.mihanizzm.ultistats.dto.response.MatchResponse
import com.github.mihanizzm.ultistats.facade.MatchFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches", description = "Управление матчами")
class MatchController(
    private val matchFacade: MatchFacade,
) {
    @GetMapping
    @Operation(summary = "Получить все матчи")
    fun getAll(): List<MatchResponse> = matchFacade.getAll()

    @GetMapping("/{id}")
    @Operation(summary = "Получить матч по ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<MatchResponse> =
        matchFacade.getById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    @Operation(summary = "Создать матч")
    fun create(@RequestBody request: CreateMatchRequest): ResponseEntity<MatchResponse> =
        matchFacade.create(request)
            ?.let { ResponseEntity.status(HttpStatus.CREATED).body(it) }
            ?: ResponseEntity.badRequest().build()

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить матч")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID): ResponseEntity<Unit> =
        if (matchFacade.delete(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
}
