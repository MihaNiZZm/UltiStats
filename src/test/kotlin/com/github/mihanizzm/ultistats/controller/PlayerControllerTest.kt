package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.dto.request.CreatePlayerRequest
import com.github.mihanizzm.ultistats.dto.request.UpdatePlayerRequest
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
class PlayerControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var playerService: PlayerService

    @Autowired
    lateinit var teamService: TeamService

    @BeforeEach
    fun setup() {
        teamService.getAll().forEach { teamService.delete(it.id) }
        playerService.getAll().forEach { playerService.delete(it.id) }
    }

    @Test
    fun `Получение игроков с пагинацией возвращает PageResponse`() {
        createTestPlayer("Иван", "Иванов")
        createTestPlayer("Пётр", "Петров")

        mockMvc.perform(get("/api/v1/players"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.currentPage").value(0))
    }

    @Test
    fun `Фильтрация игроков по имени работает`() {
        createTestPlayer("Иван", "Иванов")
        createTestPlayer("Пётр", "Иванов")
        createTestPlayer("Сергей", "Сергеев")

        mockMvc.perform(get("/api/v1/players").param("name", "Иванов"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `Фильтрация игроков по команде работает`() {
        val team = createTestTeam()
        createTestPlayer("Иван", "Иванов", team.id)
        createTestPlayer("Пётр", "Петров", team.id)
        createTestPlayer("Сергей", "Сергеев", null)

        mockMvc.perform(get("/api/v1/players").param("teamId", team.id.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `Пагинация игроков работает`() {
        for (i in 1..5) {
            createTestPlayer("Игрок$i", "Фамилия$i")
        }

        mockMvc.perform(
            get("/api/v1/players")
                .param("page", "0")
                .param("size", "2")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.currentPage").value(0))

        mockMvc.perform(
            get("/api/v1/players")
                .param("page", "2")
                .param("size", "2")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.currentPage").value(2))
    }

    @Test
    fun `Комбинированная фильтрация работает`() {
        val team = createTestTeam()
        createTestPlayer("Алексей", "Смирнов", team.id)
        createTestPlayer("Алексей", "Козлов", null)
        createTestPlayer("Пётр", "Петров", team.id)

        mockMvc.perform(
            get("/api/v1/players")
                .param("name", "Алексей")
                .param("teamId", team.id.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].firstName").value("Алексей"))
            .andExpect(jsonPath("$.content[0].lastName").value("Смирнов"))
    }

    @Test
    fun `Создание игрока возвращает 201`() {
        val request = CreatePlayerRequest(
            number = 10,
            firstName = "Иван",
            lastName = "Иванов",
        )

        mockMvc.perform(
            post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.firstName").value("Иван"))
            .andExpect(jsonPath("$.lastName").value("Иванов"))
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    fun `Получение игрока по ID возвращает 200`() {
        val player = createTestPlayer("Иван", "Иванов")

        mockMvc.perform(get("/api/v1/players/${player.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Иван"))
    }

    @Test
    fun `Получение несуществующего игрока возвращает 404`() {
        mockMvc.perform(get("/api/v1/players/${UUID.randomUUID()}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Удаление игрока возвращает 204`() {
        val player = createTestPlayer("Иван", "Иванов")

        mockMvc.perform(delete("/api/v1/players/${player.id}"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/v1/players/${player.id}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Сортировка по умолчанию работает (lastName, firstName)`() {
        createTestPlayerWithNumber("Борис", "Яковлев", 1)
        createTestPlayerWithNumber("Анна", "Иванова", 2)
        createTestPlayerWithNumber("Виктор", "Иванов", 3)

        mockMvc.perform(get("/api/v1/players"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].lastName").value("Иванов"))
            .andExpect(jsonPath("$.content[0].firstName").value("Виктор"))
            .andExpect(jsonPath("$.content[1].lastName").value("Иванова"))
            .andExpect(jsonPath("$.content[2].lastName").value("Яковлев"))
    }

    @Test
    fun `Сортировка по номеру работает`() {
        val team = createTestTeam()
        createTestPlayerWithNumber("Игрок", "Первый", 10, team.id)
        createTestPlayerWithNumber("Игрок", "Второй", 5, team.id)
        createTestPlayerWithNumber("Игрок", "Третий", 15, team.id)

        mockMvc.perform(
            get("/api/v1/players")
                .param("sort", "number:asc")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].number").value(5))
            .andExpect(jsonPath("$.content[1].number").value(10))
            .andExpect(jsonPath("$.content[2].number").value(15))
    }

    @Test
    fun `Сортировка по убыванию работает`() {
        createTestPlayerWithNumber("Игрок", "Альфа", 1)
        createTestPlayerWithNumber("Игрок", "Бета", 2)
        createTestPlayerWithNumber("Игрок", "Гамма", 3)

        mockMvc.perform(
            get("/api/v1/players")
                .param("sort", "lastName:desc")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].lastName").value("Гамма"))
            .andExpect(jsonPath("$.content[1].lastName").value("Бета"))
            .andExpect(jsonPath("$.content[2].lastName").value("Альфа"))
    }

    @Test
    fun `Список игроков содержит название команды`() {
        val team = createTestTeam("Команда Альфа")
        createTestPlayer("Иван", "Иванов", team.id)

        mockMvc.perform(get("/api/v1/players"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].teamName").value("Команда Альфа"))
            .andExpect(jsonPath("$.content[0].firstName").value("Иван"))
            .andExpect(jsonPath("$.content[0].lastName").value("Иванов"))
    }

    @Test
    fun `Список игроков содержит null для teamName у игрока без команды`() {
        createTestPlayer("Иван", "Иванов", null)

        mockMvc.perform(get("/api/v1/players"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].teamName").doesNotExist())
            .andExpect(jsonPath("$.content[0].firstName").value("Иван"))
    }

    @Test
    fun `Список игроков содержит номер игрока`() {
        val team = createTestTeam()
        createTestPlayerWithNumber("Иван", "Иванов", 42, team.id)

        mockMvc.perform(get("/api/v1/players"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].number").value(42))
    }

    @Test
    fun `Список игроков не содержит teamId`() {
        val team = createTestTeam("Команда")
        createTestPlayer("Иван", "Иванов", team.id)

        mockMvc.perform(get("/api/v1/players"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].teamId").doesNotExist())
    }

    @Test
    fun `Частичное обновление игрока (только firstName) работает`() {
        val player = createTestPlayer("Иван", "Иванов")

        val updateRequest = UpdatePlayerRequest(firstName = "Обновлённый")

        mockMvc.perform(
            put("/api/v1/players/${player.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Обновлённый"))
            .andExpect(jsonPath("$.lastName").value("Иванов"))
    }

    @Test
    fun `Частичное обновление игрока (несколько полей) работает`() {
        val team = createTestTeam()
        val player = createTestPlayerWithNumber("Иван", "Иванов", 10, team.id)

        val updateRequest = UpdatePlayerRequest(
            firstName = "Пётр",
            number = 99
        )

        mockMvc.perform(
            put("/api/v1/players/${player.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Пётр"))
            .andExpect(jsonPath("$.lastName").value("Иванов"))
            .andExpect(jsonPath("$.number").value(99))
    }

    @Test
    fun `Обновление несуществующего игрока возвращает 404`() {
        val updateRequest = UpdatePlayerRequest(firstName = "Test")

        mockMvc.perform(
            put("/api/v1/players/${UUID.randomUUID()}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)
    }

    private fun createTestPlayerWithNumber(
        firstName: String,
        lastName: String,
        number: Int,
        teamId: UUID? = null,
    ): Player {
        val player = Player(
            id = UUID.randomUUID(),
            teamId = teamId,
            number = number,
            firstName = firstName,
            lastName = lastName,
        )
        playerService.create(player)
        return player
    }

    private fun createTestPlayer(firstName: String, lastName: String, teamId: UUID? = null): Player {
        val player = Player(
            id = UUID.randomUUID(),
            teamId = teamId,
            number = (1..99).random(),
            firstName = firstName,
            lastName = lastName,
        )
        playerService.create(player)
        return player
    }

    private fun createTestTeam(name: String = "Test Team"): Team {
        val team = Team(
            id = UUID.randomUUID(),
            name = name,
            playerIds = emptyList(),
        )
        teamService.create(team)
        return team
    }
}
