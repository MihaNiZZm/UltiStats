package com.github.mihanizzm.ultistats.controller

import com.github.mihanizzm.ultistats.dto.request.CreateMatchRequest
import com.github.mihanizzm.ultistats.dto.response.MatchResponse
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.TeamService
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
    private val matchService: MatchService,
    private val teamService: TeamService,
) {
    @GetMapping
    @Operation(summary = "Получить все матчи")
    fun getAll(): List<MatchResponse> =
        matchService.getAll().map { MatchResponse.from(it) }

    @GetMapping("/{id}")
    @Operation(summary = "Получить матч по ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<MatchResponse> {
        val match = matchService.get(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(MatchResponse.from(match))
    }

    @PostMapping
    @Operation(summary = "Создать матч")
    fun create(@RequestBody request: CreateMatchRequest): ResponseEntity<MatchResponse> {
        val teams = teamService.getAllInList(request.teamIds)
        if (teams.size != request.teamIds.size) {
            return ResponseEntity.badRequest().build()
        }
        val match = Match(
            id = UUID.randomUUID(),
            teams = teams,
        )
        matchService.create(match)
        return ResponseEntity.status(HttpStatus.CREATED).body(MatchResponse.from(match))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить матч")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID): ResponseEntity<Unit> {
        if (matchService.get(id) == null) {
            return ResponseEntity.notFound().build()
        }
        matchService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
