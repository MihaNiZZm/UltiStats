package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.events.PassEvent
import com.github.mihanizzm.ultistats.model.events.TurnoverEvent
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Suppress("NonAsciiCharacters")
class StatisticsControllerTest {
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

    @Autowired
    lateinit var eventService: EventService

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
    fun `Получение статистики пустого матча возвращает 200`() {
        mockMvc.perform(get("/api/matches/${match.id}/statistics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.playerStatistics").isArray)
            .andExpect(jsonPath("$.teamStatistics").isArray)
    }

    @Test
    fun `Получение статистики несуществующего матча возвращает 404`() {
        mockMvc.perform(get("/api/matches/${UUID.randomUUID()}/statistics"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Статистика учитывает события матча`() {
        val player1 = players1[0]
        val player2 = players1[1]

        // Добавляем события: подбор и пас
        eventService.create(
            TurnoverEvent(player1.id, team1.id, Instant.now()),
            match.id
        )
        eventService.create(
            PassEvent(player1.id, player2.id, team1.id, team1.id, Instant.now()),
            match.id
        )

        mockMvc.perform(get("/api/matches/${match.id}/statistics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.teamStatistics[?(@.teamId=='${team1.id}')].attack.allPasses").value(1))
            .andExpect(jsonPath("$.teamStatistics[?(@.teamId=='${team1.id}')].attack.completePasses").value(1))
    }

    private fun createTestTeam(name: String): Pair<Team, List<Player>> {
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
        return team to players
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
