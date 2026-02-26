package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.dto.request.CreateMatchRequest
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
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
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Suppress("NonAsciiCharacters")
class MatchControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var matchService: MatchService

    @Autowired
    lateinit var teamService: TeamService

    @Autowired
    lateinit var playerService: PlayerService

    @BeforeEach
    fun setup() {
        matchService.getAll().forEach { matchService.delete(it.id) }
        teamService.getAll().forEach { teamService.delete(it.id) }
        playerService.getAll().forEach { playerService.delete(it.id) }
    }

    @Test
    fun `Создание матча возвращает 201`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val request = CreateMatchRequest(teamIds = listOf(team1.id, team2.id))

        mockMvc.perform(
            post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.teams.length()").value(2))
            .andExpect(jsonPath("$.eventCount").value(0))
            .andExpect(jsonPath("$.diskHolderId").doesNotExist())
    }

    @Test
    fun `Создание матча с несуществующей командой возвращает 400`() {
        val team1 = createTestTeam("Команда 1")
        val request = CreateMatchRequest(teamIds = listOf(team1.id, UUID.randomUUID()))

        mockMvc.perform(
            post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `Получение матча по ID возвращает 200`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val request = CreateMatchRequest(teamIds = listOf(team1.id, team2.id))

        val result = mockMvc.perform(
            post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn()

        val matchId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        mockMvc.perform(get("/api/v1/matches/$matchId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(matchId))
    }

    @Test
    fun `Получение несуществующего матча возвращает 404`() {
        mockMvc.perform(get("/api/v1/matches/${UUID.randomUUID()}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Удаление матча возвращает 204`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val request = CreateMatchRequest(teamIds = listOf(team1.id, team2.id))

        val result = mockMvc.perform(
            post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn()

        val matchId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        mockMvc.perform(delete("/api/v1/matches/$matchId"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/v1/matches/$matchId"))
            .andExpect(status().isNotFound)
    }

    private fun createTestTeam(name: String): Team {
        val teamId = UUID.randomUUID()
        val players = listOf(
            Player(UUID.randomUUID(), teamId, 1, "Игрок", "Один"),
            Player(UUID.randomUUID(), teamId, 2, "Игрок", "Два"),
        )
        players.forEach { playerService.create(it) }

        val team = Team(
            id = teamId,
            name = name,
            playerIds = players.map { it.id }
        )
        teamService.create(team)
        return team
    }
}
