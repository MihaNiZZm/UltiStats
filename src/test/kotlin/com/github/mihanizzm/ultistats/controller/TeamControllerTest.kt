package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.dto.request.CreateTeamRequest
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
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
class TeamControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var teamService: TeamService

    @Autowired
    lateinit var playerService: PlayerService

    @BeforeEach
    fun setup() {
        teamService.getAll().forEach { teamService.delete(it.id) }
        playerService.getAll().forEach { playerService.delete(it.id) }
    }

    @Test
    fun `Создание команды возвращает 201`() {
        val player = Player(
            id = UUID.randomUUID(),
            teamId = null,
            number = 10,
            firstName = "Иван",
            lastName = "Иванов",
        )
        playerService.create(player)

        val request = CreateTeamRequest(
            name = "Test Team",
            playerIds = listOf(player.id),
        )

        mockMvc.perform(
            post("/api/v1/teams")
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
        val (team, _) = createTestTeam()

        mockMvc.perform(get("/api/v1/teams/${team.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(team.name))
    }

    @Test
    fun `Получение несуществующей команды возвращает 404`() {
        mockMvc.perform(get("/api/v1/teams/${UUID.randomUUID()}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Получение всех команд с пагинацией возвращает PageResponse`() {
        createTestTeam("Team 1")
        createTestTeam("Team 2")

        mockMvc.perform(get("/api/v1/teams"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.currentPage").value(0))
    }

    @Test
    fun `Фильтрация команд по имени работает`() {
        createTestTeam("Alpha Team")
        createTestTeam("Beta Team")
        createTestTeam("Gamma")

        mockMvc.perform(get("/api/v1/teams").param("name", "Team"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `Пагинация команд работает`() {
        for (i in 1..5) {
            createTestTeam("Team $i")
        }

        mockMvc.perform(
            get("/api/v1/teams")
                .param("page", "0")
                .param("size", "2")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.currentPage").value(0))

        mockMvc.perform(
            get("/api/v1/teams")
                .param("page", "2")
                .param("size", "2")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.currentPage").value(2))
    }

    @Test
    fun `Удаление команды возвращает 204`() {
        val (team, _) = createTestTeam()

        mockMvc.perform(delete("/api/v1/teams/${team.id}"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/v1/teams/${team.id}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Добавление игрока в команду возвращает 201`() {
        val (team, players) = createTestTeam()

        val newPlayer = Player(
            id = UUID.randomUUID(),
            teamId = null,
            number = 99,
            firstName = "Новый",
            lastName = "Игрок",
        )
        playerService.create(newPlayer)

        mockMvc.perform(post("/api/v1/teams/${team.id}/players/${newPlayer.id}"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.players.length()").value(players.size + 1))
    }

    @Test
    fun `Удаление игрока из команды возвращает 200`() {
        val (team, players) = createTestTeam()
        val playerId = players.first().id

        mockMvc.perform(delete("/api/v1/teams/${team.id}/players/$playerId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.players.length()").value(players.size - 1))
    }

    @Test
    fun `Сортировка команд по имени работает`() {
        createTestTeam("Gamma Team")
        createTestTeam("Alpha Team")
        createTestTeam("Beta Team")

        mockMvc.perform(get("/api/v1/teams"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].name").value("Alpha Team"))
            .andExpect(jsonPath("$.content[1].name").value("Beta Team"))
            .andExpect(jsonPath("$.content[2].name").value("Gamma Team"))
    }

    @Test
    fun `Сортировка команд по убыванию работает`() {
        createTestTeam("Alpha")
        createTestTeam("Beta")
        createTestTeam("Gamma")

        mockMvc.perform(
            get("/api/v1/teams")
                .param("sort", "name:desc")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].name").value("Gamma"))
            .andExpect(jsonPath("$.content[1].name").value("Beta"))
            .andExpect(jsonPath("$.content[2].name").value("Alpha"))
    }

    private fun createTestTeam(name: String = "Test Team"): Pair<Team, List<Player>> {
        val teamId = UUID.randomUUID()
        val players = listOf(
            Player(
                id = UUID.randomUUID(),
                teamId = teamId,
                number = 10,
                firstName = "Тест",
                lastName = "Игрок",
            )
        )
        players.forEach { playerService.create(it) }

        val team = Team(
            id = teamId,
            name = name,
            playerIds = players.map { it.id }
        )
        teamService.create(team)
        return team to players
    }
}
