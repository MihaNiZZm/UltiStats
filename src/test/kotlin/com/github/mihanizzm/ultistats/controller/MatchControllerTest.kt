package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.dto.request.CreateMatchRequest
import com.github.mihanizzm.ultistats.dto.request.MatchTimestampRequest
import com.github.mihanizzm.ultistats.model.Match
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
import java.time.Instant
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

    @Test
    fun `Получение матчей с пагинацией возвращает PageResponse`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        createTestMatch(team1, team2)
        createTestMatch(team1, team2)

        mockMvc.perform(get("/api/v1/matches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.currentPage").value(0))
    }

    @Test
    fun `Фильтрация матчей по команде работает`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val team3 = createTestTeam("Команда 3")

        createTestMatch(team1, team2)
        createTestMatch(team1, team3)
        createTestMatch(team2, team3)

        mockMvc.perform(get("/api/v1/matches").param("teamId", team1.id.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `Фильтрация матчей по статусу работает`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")

        val plannedMatch = createTestMatch(team1, team2)
        val inProgressMatch = createTestMatch(team1, team2)
        matchService.startMatch(inProgressMatch.id, Instant.now())

        mockMvc.perform(get("/api/v1/matches").param("status", "PLANNED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))

        mockMvc.perform(get("/api/v1/matches").param("status", "IN_PROGRESS"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `Пагинация матчей работает`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")

        for (i in 1..5) {
            createTestMatch(team1, team2)
        }

        mockMvc.perform(
            get("/api/v1/matches")
                .param("page", "0")
                .param("size", "2")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.currentPage").value(0))

        mockMvc.perform(
            get("/api/v1/matches")
                .param("page", "2")
                .param("size", "2")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.currentPage").value(2))
    }

    @Test
    fun `Сортировка по умолчанию работает (plannedStartTimestamp)`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")

        val now = Instant.now()
        createTestMatchWithTimestamp(team1, team2, now.plusSeconds(3600)) // через час
        createTestMatchWithTimestamp(team1, team2, now.plusSeconds(1800)) // через 30 мин
        createTestMatchWithTimestamp(team1, team2, now.plusSeconds(7200)) // через 2 часа

        mockMvc.perform(get("/api/v1/matches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(3))
            // По умолчанию ASC - сначала ближайший
            .andExpect(jsonPath("$.content[0].plannedStartTimestamp").exists())
    }

    @Test
    fun `Сортировка по plannedStartTimestamp по убыванию работает`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")

        val now = Instant.now()
        val match1 = createTestMatchWithTimestamp(team1, team2, now.plusSeconds(1000))
        val match2 = createTestMatchWithTimestamp(team1, team2, now.plusSeconds(2000))
        val match3 = createTestMatchWithTimestamp(team1, team2, now.plusSeconds(3000))

        mockMvc.perform(
            get("/api/v1/matches")
                .param("sort", "plannedStartTimestamp:desc")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(match3.id.toString()))
            .andExpect(jsonPath("$.content[1].id").value(match2.id.toString()))
            .andExpect(jsonPath("$.content[2].id").value(match1.id.toString()))
    }

    @Test
    fun `Сортировка по статусу работает`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")

        // PLANNED
        createTestMatch(team1, team2)

        // IN_PROGRESS
        val inProgressMatch = createTestMatch(team1, team2)
        matchService.startMatch(inProgressMatch.id, Instant.now())

        // FINISHED
        val finishedMatch = createTestMatch(team1, team2)
        matchService.startMatch(finishedMatch.id, Instant.now())
        matchService.endMatch(finishedMatch.id, Instant.now())

        mockMvc.perform(
            get("/api/v1/matches")
                .param("sort", "status:asc")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(3))
            // FINISHED < IN_PROGRESS < PLANNED (alphabetically)
            .andExpect(jsonPath("$.content[0].status").value("FINISHED"))
            .andExpect(jsonPath("$.content[1].status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.content[2].status").value("PLANNED"))
    }

    private fun createTestMatchWithTimestamp(team1: Team, team2: Team, plannedStart: Instant): Match {
        val match = Match(
            id = UUID.randomUUID(),
            teams = listOf(team1, team2),
            plannedStartTimestamp = plannedStart,
        )
        matchService.create(match)
        return match
    }

    private fun createTestMatch(team1: Team, team2: Team): Match {
        val match = Match(
            id = UUID.randomUUID(),
            teams = listOf(team1, team2),
        )
        matchService.create(match)
        return match
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
