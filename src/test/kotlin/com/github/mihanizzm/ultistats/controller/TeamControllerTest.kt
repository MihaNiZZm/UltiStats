package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.dto.request.CreatePlayerRequest
import com.github.mihanizzm.ultistats.dto.request.CreateTeamRequest
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
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
class TeamControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var teamService: TeamService

    @BeforeEach
    fun setup() {
        teamService.getAll().forEach { teamService.delete(it.id) }
    }

    @Test
    fun `Создание команды возвращает 201`() {
        val request = CreateTeamRequest(
            name = "Test Team",
            players = listOf(
                CreatePlayerRequest(number = 10, firstName = "Иван", lastName = "Иванов"),
            )
        )

        mockMvc.perform(
            post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Test Team"))
            .andExpect(jsonPath("$.players[0].firstName").value("Иван"))
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    fun `Получение команды по ID возвращает 200`() {
        val team = createTestTeam()

        mockMvc.perform(get("/api/teams/${team.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(team.name))
    }

    @Test
    fun `Получение несуществующей команды возвращает 404`() {
        mockMvc.perform(get("/api/teams/${UUID.randomUUID()}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Получение всех команд возвращает список`() {
        createTestTeam("Team 1")
        createTestTeam("Team 2")

        mockMvc.perform(get("/api/teams"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `Удаление команды возвращает 204`() {
        val team = createTestTeam()

        mockMvc.perform(delete("/api/teams/${team.id}"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/teams/${team.id}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Добавление игрока в команду возвращает 201`() {
        val team = createTestTeam()
        val request = CreatePlayerRequest(number = 99, firstName = "Новый", lastName = "Игрок")

        mockMvc.perform(
            post("/api/teams/${team.id}/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.players.length()").value(2))
    }

    @Test
    fun `Удаление игрока из команды возвращает 200`() {
        val team = createTestTeam()
        val playerId = team.players.first().id

        mockMvc.perform(delete("/api/teams/${team.id}/players/$playerId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.players.length()").value(0))
    }

    private fun createTestTeam(name: String = "Test Team"): Team {
        val teamId = UUID.randomUUID()
        val team = Team(
            id = teamId,
            name = name,
            players = listOf(
                Player(
                    id = UUID.randomUUID(),
                    teamId = teamId,
                    number = 10,
                    firstName = "Тест",
                    lastName = "Игрок",
                )
            )
        )
        teamService.create(team)
        return team
    }
}
