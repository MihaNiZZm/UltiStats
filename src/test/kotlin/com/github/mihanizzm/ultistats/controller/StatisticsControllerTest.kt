package com.github.mihanizzm.ultistats.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.MatchParticipantKind
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.SystemEvent
import com.github.mihanizzm.ultistats.model.events.TeamEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.TeamService
import com.github.mihanizzm.ultistats.service.TeamPlayerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Duration
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

    @Autowired
    lateinit var teamPlayerService: TeamPlayerService

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
        matchService.startMatch(match.id, Instant.parse("2025-01-01T00:00:00Z"))
    }

    @Test
    fun `Получение статистики пустого матча возвращает 200`() {
        mockMvc.perform(get("/api/v1/matches/${match.id}/statistics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.playerStatistics").isArray)
            .andExpect(jsonPath("$.teamStatistics").isArray)
    }

    @Test
    fun `Статистика использует контракт участников матча и включает неизвестных`() {
        val participants = matchService.getOrThrow(match.id).participantsByTeam.values.flatten()
        val unknownParticipantIds = participants
            .filter { it.kind == MatchParticipantKind.UNKNOWN }
            .map { it.participantId.toString() }
            .toSet()

        val response = mockMvc.perform(get("/api/v1/matches/${match.id}/statistics"))
            .andExpect(status().isOk)
            .andReturn()
        val playerStatistics = objectMapper.readTree(response.response.contentAsString).get("playerStatistics")

        assertThat(playerStatistics).allSatisfy { statistics ->
            assertThat(statistics.hasNonNull("participantId")).isTrue()
            assertThat(statistics.has("playerId")).isFalse()
        }
        assertThat(playerStatistics.map { it.get("participantId").asText() }).containsExactlyInAnyOrderElementsOf(
            participants.map { it.participantId.toString() },
        )
        assertThat(playerStatistics.map { it.get("participantId").asText() }.toSet())
            .containsAll(unknownParticipantIds)
        assertThat(unknownParticipantIds).hasSize(4)
    }

    @Test
    fun `Получение статистики несуществующего матча возвращает 404`() {
        mockMvc.perform(get("/api/v1/matches/${UUID.randomUUID()}/statistics"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Статистика учитывает события матча`() {
        val player1 = players1[0]
        val player2 = players1[1]

        // Добавляем события: подбор и пас
        eventService.create(
            OnePlayerEvent(player1.id, Instant.now(), EventType.PICKUP),
            match.id
        )
        eventService.create(
            TwoPlayerEvent(player1.id, player2.id, Instant.now(), EventType.PASS),
            match.id
        )

        mockMvc.perform(get("/api/v1/matches/${match.id}/statistics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.teamStatistics[?(@.teamId=='${team1.id}')].attack.allPasses").value(1))
            .andExpect(jsonPath("$.teamStatistics[?(@.teamId=='${team1.id}')].attack.completePasses").value(1))
    }

    @Test
    fun `Duration сериализуется в ISO-8601 строку`() {
        // Создаём события с таймаутом для проверки timeStatistics
        val now = Instant.now()
        eventService.create(OnePlayerEvent(players1[0].id, now, EventType.PULL), match.id)
        eventService.create(OnePlayerEvent(players2[0].id, now.plusSeconds(10), EventType.PICKUP), match.id)
        eventService.create(TeamEvent(team2.id, now.plusSeconds(15), EventType.TIMEOUT_START), match.id)
        eventService.create(TeamEvent(team2.id, now.plusSeconds(75), EventType.TIMEOUT_END), match.id)
        eventService.create(TwoPlayerEvent(players2[0].id, players2[1].id, now.plusSeconds(90), EventType.PASS), match.id)
        eventService.create(TwoPlayerEvent(players2[1].id, players2[2].id, now.plusSeconds(100), EventType.GOAL), match.id)

        val result = mockMvc.perform(get("/api/v1/matches/${match.id}/statistics"))
            .andExpect(status().isOk)
            .andReturn()

        val json = result.response.contentAsString
        val tree = objectMapper.readTree(json)

        // Проверяем, что timeStatistics поля - строки в ISO-8601 формате
        val timeStats = tree.get("timeStatistics")
        assertThat(timeStats.get("timeSpentOnTimeouts").isTextual).isTrue()
        assertThat(timeStats.get("timeSpentOnTimeouts").asText()).isNotEmpty()
        assertThat(timeStats.get("timeSpentBetweenPoints").isTextual).isTrue()
        assertThat(timeStats.get("pureGameTime").isTextual).isTrue()

        // Проверяем, что поля времени в teamStatistics тоже строки
        val team2Stats = tree.get("teamStatistics").find { it.get("teamId").asText() == team2.id.toString() }
        assertThat(team2Stats?.get("time")?.get("totalPossessionTime")?.isTextual).isTrue()
        assertThat(team2Stats?.get("time")?.get("totalTimeSpentOnTimeouts")?.isTextual).isTrue()

        // Проверяем, что поля времени в playerStatistics тоже строки
        val playerStats = tree.get("playerStatistics").first()
        assertThat(playerStats.get("time").get("totalPossessionTime").isTextual).isTrue()
    }

    private fun createTestTeam(name: String): Pair<Team, List<Player>> {
        val teamId = UUID.randomUUID()
        val players = listOf(
            Player(UUID.randomUUID(), "Игрок", "Один"),
            Player(UUID.randomUUID(), "Игрок", "Два"),
            Player(UUID.randomUUID(), "Игрок", "Три"),
        )
        val team = Team(
            id = teamId,
            name = name,
        )
        teamService.create(team)
        players.forEach { playerService.create(it) }
        players.forEachIndexed { index, player -> teamPlayerService.add(teamId, player.id, index + 1) }
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
