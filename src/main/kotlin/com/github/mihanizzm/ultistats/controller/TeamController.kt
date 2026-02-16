package com.github.mihanizzm.ultistats.controller

import com.github.mihanizzm.ultistats.dto.request.CreatePlayerRequest
import com.github.mihanizzm.ultistats.dto.request.CreateTeamRequest
import com.github.mihanizzm.ultistats.dto.response.TeamResponse
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.service.TeamService
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
    private val teamService: TeamService,
) {
    @GetMapping
    @Operation(summary = "Получить все команды")
    fun getAll(): List<TeamResponse> =
        teamService.getAll().map { TeamResponse.from(it) }

    @GetMapping("/{id}")
    @Operation(summary = "Получить команду по ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<TeamResponse> {
        val team = teamService.get(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(TeamResponse.from(team))
    }

    @PostMapping
    @Operation(summary = "Создать команду")
    fun create(@RequestBody request: CreateTeamRequest): ResponseEntity<TeamResponse> {
        val teamId = UUID.randomUUID()
        val players = request.players.map { playerRequest ->
            Player(
                id = UUID.randomUUID(),
                teamId = teamId,
                number = playerRequest.number,
                firstName = playerRequest.firstName,
                lastName = playerRequest.lastName,
            )
        }
        val team = Team(
            id = teamId,
            name = request.name,
            players = players,
        )
        teamService.create(team)
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(team))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить команду")
    fun update(@PathVariable id: UUID, @RequestBody request: CreateTeamRequest): ResponseEntity<TeamResponse> {
        val existingTeam = teamService.get(id) ?: return ResponseEntity.notFound().build()
        val players = request.players.map { playerRequest ->
            Player(
                id = UUID.randomUUID(),
                teamId = id,
                number = playerRequest.number,
                firstName = playerRequest.firstName,
                lastName = playerRequest.lastName,
            )
        }
        val updatedTeam = existingTeam.copy(
            name = request.name,
            players = players,
        )
        teamService.delete(id)
        teamService.create(updatedTeam)
        return ResponseEntity.ok(TeamResponse.from(updatedTeam))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить команду")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID): ResponseEntity<Unit> {
        if (teamService.get(id) == null) {
            return ResponseEntity.notFound().build()
        }
        teamService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{teamId}/players")
    @Operation(summary = "Добавить игрока в команду")
    fun addPlayer(
        @PathVariable teamId: UUID,
        @RequestBody request: CreatePlayerRequest
    ): ResponseEntity<TeamResponse> {
        val team = teamService.get(teamId) ?: return ResponseEntity.notFound().build()
        val newPlayer = Player(
            id = UUID.randomUUID(),
            teamId = teamId,
            number = request.number,
            firstName = request.firstName,
            lastName = request.lastName,
        )
        val updatedTeam = team.copy(players = team.players + newPlayer)
        teamService.delete(teamId)
        teamService.create(updatedTeam)
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(updatedTeam))
    }

    @DeleteMapping("/{teamId}/players/{playerId}")
    @Operation(summary = "Удалить игрока из команды")
    fun removePlayer(
        @PathVariable teamId: UUID,
        @PathVariable playerId: UUID
    ): ResponseEntity<TeamResponse> {
        val team = teamService.get(teamId) ?: return ResponseEntity.notFound().build()
        if (!team.hasPlayer(playerId)) {
            return ResponseEntity.notFound().build()
        }
        val updatedTeam = team.copy(players = team.players.filter { it.id != playerId })
        teamService.delete(teamId)
        teamService.create(updatedTeam)
        return ResponseEntity.ok(TeamResponse.from(updatedTeam))
    }
}
