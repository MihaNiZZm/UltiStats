package com.github.mihanizzm.ultistats.controller

import com.github.mihanizzm.ultistats.dto.request.CreatePlayerRequest
import com.github.mihanizzm.ultistats.dto.response.PlayerResponse
import com.github.mihanizzm.ultistats.facade.PlayerFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/players")
@Tag(name = "Players", description = "Управление игроками")
class PlayerController(
    private val playerFacade: PlayerFacade,
) {
    @GetMapping
    @Operation(summary = "Получить всех игроков")
    fun getAll(): List<PlayerResponse> = playerFacade.getAll()

    @GetMapping("/{id}")
    @Operation(summary = "Получить игрока по ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<PlayerResponse> =
        playerFacade.getById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    @Operation(summary = "Создать игрока")
    fun create(@RequestBody request: CreatePlayerRequest): ResponseEntity<PlayerResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(playerFacade.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Обновить игрока")
    fun update(@PathVariable id: UUID, @RequestBody request: CreatePlayerRequest): ResponseEntity<PlayerResponse> =
        playerFacade.update(id, request)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить игрока")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID): ResponseEntity<Unit> =
        if (playerFacade.delete(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
}
