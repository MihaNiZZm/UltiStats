package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamPlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var matchService: MatchService
    @Autowired lateinit var teamService: TeamService
    @Autowired lateinit var playerService: PlayerService
    @Autowired lateinit var teamPlayerService: TeamPlayerService

    private lateinit var match: Match
    private lateinit var team1: Team
    private lateinit var team2: Team
    private lateinit var player1: Player
    private lateinit var player2: Player
    private lateinit var opponent: Player

    @BeforeEach
    fun setUp() {
        matchService.getAll().forEach { matchService.delete(it.id) }
        teamService.getAll().forEach { teamService.delete(it.id) }
        playerService.getAll().forEach { playerService.delete(it.id) }
        team1 = Team(UUID.randomUUID(), "One")
        team2 = Team(UUID.randomUUID(), "Two")
        teamService.create(team1); teamService.create(team2)
        player1 = Player(UUID.randomUUID(), "First", "Player")
        player2 = Player(UUID.randomUUID(), "Second", "Player")
        opponent = Player(UUID.randomUUID(), "Other", "Player")
        listOf(player1, player2, opponent).forEach(playerService::create)
        teamPlayerService.add(team1.id, player1.id, 1)
        teamPlayerService.add(team1.id, player2.id, 2)
        teamPlayerService.add(team2.id, opponent.id, 3)
        match = Match(UUID.randomUUID(), listOf(team1.id, team2.id))
        matchService.create(match)
    }

    @Test
    fun `one-player event has only its category fields`() {
        mockMvc.perform(postEvent(mapOf("type" to "TURNOVER", "occurredAt" to "2026-07-14T10:00:00Z", "playerId" to player1.id)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.sequenceNumber").value(1))
            .andExpect(jsonPath("$.playerId").value(player1.id.toString()))
            .andExpect(jsonPath("$.teamId").doesNotExist())
            .andExpect(jsonPath("$._eventClass").doesNotExist())
    }

    @Test
    fun `two-player request rejects players from wrong team for pass`() {
        mockMvc.perform(postEvent(mapOf(
            "type" to "PASS", "occurredAt" to "2026-07-14T10:00:00Z",
            "fromPlayerId" to player1.id, "toPlayerId" to opponent.id,
        ))).andExpect(status().isBadRequest)
    }

    @Test
    fun `team and system event schemas deserialize by type`() {
        mockMvc.perform(postEvent(mapOf("type" to "TIMEOUT_START", "occurredAt" to "2026-07-14T10:00:00Z", "teamId" to team1.id)))
            .andExpect(status().isCreated).andExpect(jsonPath("$.teamId").value(team1.id.toString()))
        mockMvc.perform(postEvent(mapOf("type" to "HALFTIME_START", "occurredAt" to "2026-07-14T10:01:00Z")))
            .andExpect(status().isCreated).andExpect(jsonPath("$.teamId").doesNotExist())
    }

    @Test
    fun `event is read patched and deleted by UUID`() {
        val created = mockMvc.perform(postEvent(mapOf("type" to "TURNOVER", "occurredAt" to "2026-07-14T10:00:00Z", "playerId" to player1.id)))
            .andReturn().response.contentAsString
        val eventId = objectMapper.readTree(created).get("id").asText()

        mockMvc.perform(get("/api/v1/matches/${match.id}/events/$eventId"))
            .andExpect(status().isOk).andExpect(jsonPath("$.playerId").value(player1.id.toString()))
        mockMvc.perform(patch("/api/v1/matches/${match.id}/events/$eventId")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapOf("type" to "TURNOVER", "playerId" to player2.id))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.playerId").value(player2.id.toString()))
            .andExpect(jsonPath("$.occurredAt").value("2026-07-14T10:00:00Z"))
        mockMvc.perform(delete("/api/v1/matches/${match.id}/events/$eventId"))
            .andExpect(status().isNoContent)
        mockMvc.perform(get("/api/v1/matches/${match.id}/events/$eventId")).andExpect(status().isNotFound)
    }

    @Test
    fun `event type cannot change and system event cannot be patched`() {
        val playerEvent = createAndGetId(mapOf("type" to "TURNOVER", "occurredAt" to "2026-07-14T10:00:00Z", "playerId" to player1.id))
        mockMvc.perform(patch("/api/v1/matches/${match.id}/events/$playerEvent")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapOf("type" to "DROP", "playerId" to player1.id))))
            .andExpect(status().isBadRequest)

        val systemEvent = createAndGetId(mapOf("type" to "HALFTIME_START", "occurredAt" to "2026-07-14T10:01:00Z"))
        mockMvc.perform(patch("/api/v1/matches/${match.id}/events/$systemEvent")
            .contentType(MediaType.APPLICATION_JSON).content("""{"type":"HALFTIME_START"}"""))
            .andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `creation rejects timestamp before previous event`() {
        createAndGetId(mapOf("type" to "TURNOVER", "occurredAt" to "2026-07-14T10:01:00Z", "playerId" to player1.id))
        mockMvc.perform(postEvent(mapOf("type" to "DROP", "occurredAt" to "2026-07-14T10:00:00Z", "playerId" to player1.id)))
            .andExpect(status().isBadRequest)
    }

    private fun postEvent(body: Map<String, Any>) = post("/api/v1/matches/${match.id}/events")
        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))

    private fun createAndGetId(body: Map<String, Any>): String = objectMapper.readTree(
        mockMvc.perform(postEvent(body)).andExpect(status().isCreated).andReturn().response.contentAsString
    ).get("id").asText()
}
