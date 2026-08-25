package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.dto.request.CreateMatchRequest
import com.github.mihanizzm.ultistats.dto.request.MatchTimestampRequest
import com.github.mihanizzm.ultistats.dto.request.UpdateMatchRequest
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import com.github.mihanizzm.ultistats.service.TeamPlayerService
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
    lateinit var eventService: EventService

    @Autowired
    lateinit var teamService: TeamService

    @Autowired
    lateinit var playerService: PlayerService

    @Autowired
    lateinit var teamPlayerService: TeamPlayerService

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
            .andExpect(jsonPath("$.teams[0].participants.length()").value(4))
            .andExpect(jsonPath("$.teams[0].participants[0].kind").value("PLAYER"))
            .andExpect(jsonPath("$.teams[0].participants[0].participantId").exists())
            .andExpect(jsonPath("$.teams[0].participants[0].playerId").doesNotExist())
            .andExpect(jsonPath("$.teams[0].participants[2].kind").value("UNKNOWN"))
            .andExpect(jsonPath("$.teams[0].participants[2].unknownSlot").value(1))
            .andExpect(jsonPath("$.teams[0].participants[2].playerId").doesNotExist())
            .andExpect(jsonPath("$.teams[0].participants[3].kind").value("UNKNOWN"))
            .andExpect(jsonPath("$.teams[0].participants[3].unknownSlot").value(2))
            .andExpect(jsonPath("$.teams[1].participants.length()").value(4))
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
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.currentStatus").doesNotExist())
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
        val finishedAt = Instant.parse("2026-08-14T10:00:00Z")
        matchService.startMatch(finishedMatch.id, finishedAt.minusSeconds(60))
        recordCompletedPoint(finishedMatch, finishedAt)
        matchService.endMatch(finishedMatch.id, finishedAt)

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

    @Test
    fun `Частичное обновление матча (только plannedStartTimestamp) работает`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)

        val newTimestamp = Instant.now().plusSeconds(3600)
        val updateRequest = UpdateMatchRequest(plannedStartTimestamp = newTimestamp)

        mockMvc.perform(
            put("/api/v1/matches/${match.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.plannedStartTimestamp").exists())
    }

    @Test
    fun `Частичное обновление матча (только teamIds) работает`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val team3 = createTestTeam("Команда 3")
        val match = createTestMatch(team1, team2)

        val updateRequest = UpdateMatchRequest(teamIds = listOf(team1.id, team3.id))

        mockMvc.perform(
            put("/api/v1/matches/${match.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.teams.length()").value(2))
    }

    @Test
    fun `Обновление несуществующего матча возвращает 404`() {
        val matchId = UUID.randomUUID()
        val updateRequest = UpdateMatchRequest(plannedStartTimestamp = Instant.now())

        mockMvc.perform(
            put("/api/v1/matches/$matchId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpectProblem(
                expectedStatus = 404,
                code = "RESOURCE_NOT_FOUND",
                instance = "/api/v1/matches/$matchId",
            )
    }

    @Test
    fun `Обновление несуществующего матча с некорректным выбором команд возвращает 404`() {
        val matchId = UUID.randomUUID()
        val updateRequest = UpdateMatchRequest(teamIds = listOf(UUID.randomUUID()))

        mockMvc.perform(
            put("/api/v1/matches/$matchId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpectProblem(
                expectedStatus = 404,
                code = "RESOURCE_NOT_FOUND",
                instance = "/api/v1/matches/$matchId",
            )
    }

    @Test
    fun `Обновление матча с некорректным выбором команд возвращает 400`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)
        val updateRequest = UpdateMatchRequest(teamIds = listOf(team1.id))

        mockMvc.perform(
            put("/api/v1/matches/${match.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpectProblem(
                expectedStatus = 400,
                code = "INVALID_REQUEST",
                instance = "/api/v1/matches/${match.id}",
            )
    }

    @Test
    fun `Обновление начатого матча с некорректным выбором команд возвращает 400`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)
        matchService.startMatch(match.id, Instant.parse("2026-08-14T10:00:00Z"))

        mockMvc.perform(
            put("/api/v1/matches/${match.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateMatchRequest(teamIds = listOf(team1.id))))
        )
            .andExpectProblem(
                expectedStatus = 400,
                code = "INVALID_REQUEST",
                instance = "/api/v1/matches/${match.id}",
            )
    }

    @Test
    fun `Обновление начатого матча возвращает состояние блокировки`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)
        matchService.startMatch(match.id, Instant.parse("2026-08-14T10:00:00Z"))

        mockMvc.perform(
            put("/api/v1/matches/${match.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateMatchRequest(plannedStartTimestamp = Instant.parse("2026-08-14T11:00:00Z"))))
        )
            .andExpectProblem(
                expectedStatus = 409,
                code = "MATCH_UPDATE_LOCKED",
                instance = "/api/v1/matches/${match.id}",
                currentStatus = "IN_PROGRESS",
            )
    }

    @Test
    fun `Начало матча возвращает сохраненную отметку времени`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)
        val timestamp = Instant.parse("2026-08-14T10:00:00Z")

        mockMvc.perform(timestampRequest(post("/api/v1/matches/${match.id}/start"), timestamp))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.startedAt").value(timestamp.toString()))
    }

    @Test
    fun `Начало несуществующего матча возвращает 404`() {
        val matchId = UUID.randomUUID()

        mockMvc.perform(timestampRequest(post("/api/v1/matches/$matchId/start"), Instant.parse("2026-08-14T10:00:00Z")))
            .andExpectProblem(
                expectedStatus = 404,
                code = "RESOURCE_NOT_FOUND",
                instance = "/api/v1/matches/$matchId/start",
            )
    }

    @Test
    fun `Повторное начало матча возвращает конфликт состояния`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)
        val timestamp = Instant.parse("2026-08-14T10:00:00Z")
        matchService.startMatch(match.id, timestamp)

        mockMvc.perform(timestampRequest(post("/api/v1/matches/${match.id}/start"), timestamp.plusSeconds(60)))
            .andExpectProblem(
                expectedStatus = 409,
                code = "MATCH_ALREADY_STARTED",
                instance = "/api/v1/matches/${match.id}/start",
                currentStatus = "IN_PROGRESS",
            )
    }

    @Test
    fun `Начало завершенного матча возвращает конфликт состояния`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)
        val timestamp = Instant.parse("2026-08-14T10:00:00Z")
        matchService.startMatch(match.id, timestamp)
        recordCompletedPoint(match, timestamp.plusSeconds(59))
        matchService.endMatch(match.id, timestamp.plusSeconds(60))

        mockMvc.perform(timestampRequest(post("/api/v1/matches/${match.id}/start"), timestamp.plusSeconds(120)))
            .andExpectProblem(
                expectedStatus = 409,
                code = "MATCH_ALREADY_FINISHED",
                instance = "/api/v1/matches/${match.id}/start",
                currentStatus = "FINISHED",
            )
    }

    @Test
    fun `Завершение матча возвращает сохраненную отметку времени`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)
        val startedAt = Instant.parse("2026-08-14T10:00:00Z")
        val endedAt = Instant.parse("2026-08-14T11:00:00Z")
        matchService.startMatch(match.id, startedAt)
        recordCompletedPoint(match, endedAt.minusSeconds(1))

        mockMvc.perform(timestampRequest(post("/api/v1/matches/${match.id}/end"), endedAt))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("FINISHED"))
            .andExpect(jsonPath("$.endedAt").value(endedAt.toString()))
    }

    @Test
    fun `Завершение матча до окончания поинта возвращает состояние лога`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)
        val startedAt = Instant.parse("2026-08-14T10:00:00Z")
        matchService.startMatch(match.id, startedAt)

        mockMvc.perform(timestampRequest(post("/api/v1/matches/${match.id}/end"), startedAt.plusSeconds(60)))
            .andExpectProblem(
                expectedStatus = 409,
                code = "MATCH_NOT_AT_POINT_END",
                instance = "/api/v1/matches/${match.id}/end",
                currentStatus = "IN_PROGRESS",
            )
            .andExpect(jsonPath("$.currentState").value("BEFORE_FIRST_PULL"))
            .andExpect(jsonPath("$.attemptedEventType").doesNotExist())
    }

    @Test
    fun `Завершение несуществующего матча возвращает 404`() {
        val matchId = UUID.randomUUID()

        mockMvc.perform(timestampRequest(post("/api/v1/matches/$matchId/end"), Instant.parse("2026-08-14T11:00:00Z")))
            .andExpectProblem(
                expectedStatus = 404,
                code = "RESOURCE_NOT_FOUND",
                instance = "/api/v1/matches/$matchId/end",
            )
    }

    @Test
    fun `Завершение незапущенного матча возвращает конфликт состояния`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)

        mockMvc.perform(timestampRequest(post("/api/v1/matches/${match.id}/end"), Instant.parse("2026-08-14T11:00:00Z")))
            .andExpectProblem(
                expectedStatus = 409,
                code = "MATCH_NOT_STARTED",
                instance = "/api/v1/matches/${match.id}/end",
                currentStatus = "PLANNED",
            )
    }

    @Test
    fun `Повторное завершение матча возвращает конфликт состояния`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)
        val startedAt = Instant.parse("2026-08-14T10:00:00Z")
        matchService.startMatch(match.id, startedAt)
        recordCompletedPoint(match, startedAt.plusSeconds(59))
        matchService.endMatch(match.id, startedAt.plusSeconds(60))

        mockMvc.perform(timestampRequest(post("/api/v1/matches/${match.id}/end"), startedAt.plusSeconds(120)))
            .andExpectProblem(
                expectedStatus = 409,
                code = "MATCH_ALREADY_FINISHED",
                instance = "/api/v1/matches/${match.id}/end",
                currentStatus = "FINISHED",
            )
    }

    @Test
    fun `Завершение до начала матча возвращает конфликт отметок времени`() {
        val team1 = createTestTeam("Команда 1")
        val team2 = createTestTeam("Команда 2")
        val match = createTestMatch(team1, team2)
        val startedAt = Instant.parse("2026-08-14T10:00:00Z")
        matchService.startMatch(match.id, startedAt)

        mockMvc.perform(timestampRequest(post("/api/v1/matches/${match.id}/end"), startedAt.minusSeconds(1)))
            .andExpectProblem(
                expectedStatus = 409,
                code = "END_BEFORE_START",
                instance = "/api/v1/matches/${match.id}/end",
            )
    }

    private fun timestampRequest(
        request: org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder,
        timestamp: Instant,
    ) = request
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(MatchTimestampRequest(timestamp)))

    private fun org.springframework.test.web.servlet.ResultActions.andExpectProblem(
        expectedStatus: Int,
        code: String,
        instance: String,
        currentStatus: String? = null,
    ) = apply {
        andExpect(status().`is`(expectedStatus))
        andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        andExpect(jsonPath("$.status").value(expectedStatus))
        andExpect(jsonPath("$.code").value(code))
        andExpect(jsonPath("$.instance").value(instance))
        if (currentStatus == null) {
            andExpect(jsonPath("$.currentStatus").doesNotExist())
        } else {
            andExpect(jsonPath("$.currentStatus").value(currentStatus))
        }
    }

    private fun createTestMatchWithTimestamp(team1: Team, team2: Team, plannedStart: Instant): Match {
        val match = Match(
            id = UUID.randomUUID(),
            teamIds = listOf(team1.id, team2.id),
            plannedStartTimestamp = plannedStart,
        )
        matchService.create(match)
        return match
    }

    private fun createTestMatch(team1: Team, team2: Team): Match {
        val match = Match(
            id = UUID.randomUUID(),
            teamIds = listOf(team1.id, team2.id),
        )
        matchService.create(match)
        return match
    }

    private fun createTestTeam(name: String): Team {
        val teamId = UUID.randomUUID()
        val players = listOf(
            Player(UUID.randomUUID(), "Игрок", "Один"),
            Player(UUID.randomUUID(), "Игрок", "Два"),
        )
        val team = Team(
            id = teamId,
            name = name,
        )
        teamService.create(team)
        players.forEach { playerService.create(it) }
        players.forEachIndexed { index, player -> teamPlayerService.add(teamId, player.id, index + 1) }
        return team
    }

    private fun recordCompletedPoint(match: Match, goalAt: Instant, type: EventType = EventType.GOAL) {
        val participants = matchService.getOrThrow(match.id).participantsByTeam.values.flatten()
        val first = participants[0].participantId
        val second = participants[1].participantId
        eventService.create(OnePlayerEvent(first, goalAt.minusSeconds(2), EventType.PULL), match.id)
        eventService.create(OnePlayerEvent(second, goalAt.minusSeconds(1), EventType.PICKUP), match.id)
        eventService.create(TwoPlayerEvent(first, second, goalAt, type), match.id)
    }
}
