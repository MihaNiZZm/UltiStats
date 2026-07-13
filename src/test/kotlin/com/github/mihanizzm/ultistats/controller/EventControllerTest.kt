package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.dto.request.UpdateEventRequest
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.events.EventType
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
class EventControllerTest {
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

    private lateinit var team1: Team
    private lateinit var team2: Team
    private lateinit var players1: List<Player>
    private lateinit var players2: List<Player>
    private lateinit var match: Match

    @BeforeEach
    fun setup() {
        matchService.getAll().forEach { matchService.delete(it.id) }
        teamService.getAll().forEach { teamService.delete(it.id) }
        playerService.getAll().forEach { playerService.delete(it.id) }

        val (t1, p1) = createTestTeam("Команда 1")
        val (t2, p2) = createTestTeam("Команда 2")
        team1 = t1
        team2 = t2
        players1 = p1
        players2 = p2
        match = createTestMatch(team1, team2)
    }

    @Test
    fun `Создание события TURNOVER возвращает eventId`() {
        val player = players1.first()
        val request = CreateEventRequest(
            type = EventType.TURNOVER,
            timestamp = Instant.now(),
            playerId = player.id,
        )

        mockMvc.perform(
            post("/api/v1/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.eventId").isNotEmpty)
    }

    @Test
    fun `Создание события PASS возвращает eventId`() {
        val player1 = players1[0]
        val player2 = players1[1]

        // Сначала подбор диска
        val turnoverRequest = CreateEventRequest(
            type = EventType.TURNOVER,
            timestamp = Instant.now(),
            playerId = player1.id,
        )
        mockMvc.perform(
            post("/api/v1/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(turnoverRequest))
        )

        // Затем пас
        val passRequest = CreateEventRequest(
            type = EventType.PASS,
            timestamp = Instant.now(),
            playerId = player1.id,
            toPlayerId = player2.id,
        )

        mockMvc.perform(
            post("/api/v1/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.eventId").isNotEmpty)
    }

    @Test
    fun `Создание события GOAL возвращает eventId`() {
        val player1 = players1[0]
        val player2 = players1[1]

        // Подбор
        createEvent(EventType.TURNOVER, player1.id)

        // Гол
        val goalRequest = CreateEventRequest(
            type = EventType.GOAL,
            timestamp = Instant.now(),
            playerId = player1.id,
            toPlayerId = player2.id,
        )

        mockMvc.perform(
            post("/api/v1/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goalRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.eventId").isNotEmpty)
    }

    @Test
    fun `Получение событий матча возвращает список`() {
        val fromPlayer = players1[0]
        val toPlayer = players1[1]
        createEvent(EventType.TURNOVER, fromPlayer.id)
        mockMvc.perform(
            post("/api/v1/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateEventRequest(
                    type = EventType.PASS,
                    timestamp = Instant.now(),
                    playerId = fromPlayer.id,
                    toPlayerId = toPlayer.id,
                )))
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/v1/matches/${match.id}/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].player").value(fromPlayer.id.toString()))
            .andExpect(jsonPath("$[0].team").doesNotExist())
            .andExpect(jsonPath("$[1].fromPlayer").value(fromPlayer.id.toString()))
            .andExpect(jsonPath("$[1].toPlayer").value(toPlayer.id.toString()))
            .andExpect(jsonPath("$[1].fromTeam").doesNotExist())
            .andExpect(jsonPath("$[1].toTeam").doesNotExist())
    }

    @Test
    fun `Удаление события возвращает eventId`() {
        val player1 = players1[0]
        val player2 = players1[1]

        // Подбор
        createEvent(EventType.TURNOVER, player1.id)

        // Пас
        val passRequest = CreateEventRequest(
            type = EventType.PASS,
            timestamp = Instant.now(),
            playerId = player1.id,
            toPlayerId = player2.id,
        )
        mockMvc.perform(
            post("/api/v1/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passRequest))
        )

        // Удаляем пас (индекс 1)
        mockMvc.perform(delete("/api/v1/matches/${match.id}/events/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.eventId").isNotEmpty)
    }

    @Test
    fun `Создание события для несуществующего матча возвращает 404`() {
        val request = CreateEventRequest(
            type = EventType.PULL,
            timestamp = Instant.now(),
            playerId = players1.first().id,
        )

        mockMvc.perform(
            post("/api/v1/matches/${UUID.randomUUID()}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Частичное обновление события (PUT только timestamp) работает`() {
        val player = players1.first()

        // Создаём событие
        createEvent(EventType.TURNOVER, player.id)

        val newTimestamp = Instant.now().plusSeconds(100)
        val updateRequest = UpdateEventRequest(timestamp = newTimestamp)

        mockMvc.perform(
            put("/api/v1/matches/${match.id}/events/0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `Частичное обновление события (PUT только type) работает`() {
        val player = players1.first()

        // Создаём событие TURNOVER
        createEvent(EventType.TURNOVER, player.id)

        val updateRequest = UpdateEventRequest(type = EventType.DROP)

        mockMvc.perform(
            put("/api/v1/matches/${match.id}/events/0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `Частичное обновление несуществующего события возвращает 404`() {
        val updateRequest = UpdateEventRequest(timestamp = Instant.now())

        mockMvc.perform(
            put("/api/v1/matches/${match.id}/events/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)
    }

    private fun createEvent(type: EventType, playerId: UUID) {
        val request = CreateEventRequest(
            type = type,
            timestamp = Instant.now(),
            playerId = playerId,
        )
        mockMvc.perform(
            post("/api/v1/matches/${match.id}/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
    }

    private fun createTestTeam(name: String): Pair<Team, List<Player>> {
        val teamId = UUID.randomUUID()
        val players = listOf(
            Player(UUID.randomUUID(), teamId, 1, "Игрок", "Один"),
            Player(UUID.randomUUID(), teamId, 2, "Игрок", "Два"),
        )
        val team = Team(
            id = teamId,
            name = name,
            playerIds = players.map { it.id }
        )
        teamService.create(team)
        players.forEach { playerService.create(it) }
        return team to players
    }

    private fun createTestMatch(team1: Team, team2: Team): Match {
        val match = Match(
            id = UUID.randomUUID(),
            teamIds = listOf(team1.id, team2.id),
        )
        matchService.create(match)
        return match
    }
}
