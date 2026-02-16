package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.TeamService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Suppress("NonAsciiCharacters")
class EventControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var matchService: MatchService

    @Autowired
    lateinit var teamService: TeamService

    private lateinit var team1: Team
    private lateinit var team2: Team
    private lateinit var match: Match

    @BeforeEach
    fun setup() {
        matchService.getAll().forEach { matchService.delete(it.id) }
        teamService.getAll().forEach { teamService.delete(it.id) }

        team1 = createTestTeam("Команда 1")
        team2 = createTestTeam("Команда 2")
        match = createTestMatch(team1, team2)
    }

    @Test
    fun `Создание события TURNOVER возвращает diskHolderId`() {
        val player = team1.players.first()
        val request = CreateEventRequest(
            type = EventType.TURNOVER,
            timestamp = Instant.now(),
            teamId = team1.id,
            playerId = player.id,
        )

        mockMvc.perform(
            post("/api/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.diskHolderId").value(player.id.toString()))
    }

    @Test
    fun `Создание события PASS обновляет diskHolderId`() {
        val player1 = team1.players[0]
        val player2 = team1.players[1]

        // Сначала подбор диска
        val turnoverRequest = CreateEventRequest(
            type = EventType.TURNOVER,
            timestamp = Instant.now(),
            teamId = team1.id,
            playerId = player1.id,
        )
        mockMvc.perform(
            post("/api/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(turnoverRequest))
        )

        // Затем пас
        val passRequest = CreateEventRequest(
            type = EventType.PASS,
            timestamp = Instant.now(),
            teamId = team1.id,
            playerId = player1.id,
            toTeamId = team1.id,
            toPlayerId = player2.id,
        )

        mockMvc.perform(
            post("/api/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.diskHolderId").value(player2.id.toString()))
    }

    @Test
    fun `Создание события GOAL сбрасывает diskHolderId`() {
        val player1 = team1.players[0]
        val player2 = team1.players[1]

        // Подбор
        createEvent(EventType.TURNOVER, team1.id, player1.id!!)

        // Гол
        val goalRequest = CreateEventRequest(
            type = EventType.GOAL,
            timestamp = Instant.now(),
            teamId = team1.id,
            playerId = player1.id,
            toTeamId = team1.id,
            toPlayerId = player2.id,
        )

        mockMvc.perform(
            post("/api/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.diskHolderId").doesNotExist())
    }

    @Test
    fun `Получение событий матча возвращает список`() {
        val player = team1.players.first()
        createEvent(EventType.TURNOVER, team1.id, player.id!!)

        mockMvc.perform(get("/api/matches/${match.id}/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `Удаление события пересчитывает diskHolderId`() {
        val player1 = team1.players[0]
        val player2 = team1.players[1]

        // Подбор
        createEvent(EventType.TURNOVER, team1.id, player1.id!!)

        // Пас
        val passRequest = CreateEventRequest(
            type = EventType.PASS,
            timestamp = Instant.now(),
            teamId = team1.id,
            playerId = player1.id,
            toTeamId = team1.id,
            toPlayerId = player2.id,
        )
        mockMvc.perform(
            post("/api/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passRequest))
        )

        // Удаляем пас (индекс 1)
        mockMvc.perform(delete("/api/matches/${match.id}/events/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.diskHolderId").value(player1.id.toString()))
    }

    @Test
    fun `Создание события для несуществующего матча возвращает 404`() {
        val request = CreateEventRequest(
            type = EventType.PULL,
            timestamp = Instant.now(),
            teamId = team1.id,
            playerId = team1.players.first().id,
        )

        mockMvc.perform(
            post("/api/matches/${UUID.randomUUID()}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    private fun createEvent(type: EventType, teamId: UUID, playerId: UUID) {
        val request = CreateEventRequest(
            type = type,
            timestamp = Instant.now(),
            teamId = teamId,
            playerId = playerId,
        )
        mockMvc.perform(
            post("/api/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
    }

    private fun createTestTeam(name: String): Team {
        val teamId = UUID.randomUUID()
        val team = Team(
            id = teamId,
            name = name,
            players = listOf(
                Player(UUID.randomUUID(), teamId, 1, "Игрок", "Один"),
                Player(UUID.randomUUID(), teamId, 2, "Игрок", "Два"),
            )
        )
        teamService.create(team)
        return team
    }

    private fun createTestMatch(team1: Team, team2: Team): Match {
        val match = Match(
            id = UUID.randomUUID(),
            teams = listOf(team1, team2),
        )
        matchService.create(match)
        return match
    }
}
