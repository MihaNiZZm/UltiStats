package com.github.mihanizzm.ultistats.controller

import com.github.mihanizzm.ultistats.dto.request.CreatePlayerRequest
import com.github.mihanizzm.ultistats.dto.request.CreateTeamRequest
import com.github.mihanizzm.ultistats.dto.response.TeamResponse
import com.github.mihanizzm.ultistats.facade.TeamFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/teams")
@Tag(name = "Teams", description = "Управление командами")
class TeamController(
    private val teamFacade: TeamFacade,
) {
    @GetMapping
    @Operation(summary = "Получить все команды")
    fun getAll(): List<TeamResponse> = teamFacade.getAll()

    @GetMapping("/{id}")
    @Operation(summary = "Получить команду по ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<TeamResponse> =
        teamFacade.getById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    @Operation(summary = "Создать команду")
    fun create(@RequestBody request: CreateTeamRequest): ResponseEntity<TeamResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(teamFacade.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Обновить команду")
    fun update(@PathVariable id: UUID, @RequestBody request: CreateTeamRequest): ResponseEntity<TeamResponse> =
        teamFacade.update(id, request)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить команду")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID): ResponseEntity<Unit> =
        if (teamFacade.delete(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    @PostMapping("/{teamId}/players")
    @Operation(summary = "Добавить игрока в команду")
    fun addPlayer(
        @PathVariable teamId: UUID,
        @RequestBody request: CreatePlayerRequest
    ): ResponseEntity<TeamResponse> =
        teamFacade.addPlayer(teamId, request)
            ?.let { ResponseEntity.status(HttpStatus.CREATED).body(it) }
            ?: ResponseEntity.notFound().build()

    @DeleteMapping("/{teamId}/players/{playerId}")
    @Operation(summary = "Удалить игрока из команды")
    fun removePlayer(
        @PathVariable teamId: UUID,
        @PathVariable playerId: UUID
    ): ResponseEntity<TeamResponse> =
        teamFacade.removePlayer(teamId, playerId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
}
