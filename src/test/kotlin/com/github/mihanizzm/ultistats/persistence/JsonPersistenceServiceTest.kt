package com.github.mihanizzm.ultistats.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.SystemEvent
import com.github.mihanizzm.ultistats.model.events.TeamEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.util.UUID

@SpringBootTest
@Suppress("NonAsciiCharacters")
class JsonPersistenceServiceTest {

    @Autowired
    lateinit var objectMapper: ObjectMapper

    companion object {
        val TEAM_1_ID: UUID = UUID.fromString("0306caac-acd3-4b0a-9c58-000000000000")
        val TEAM_2_ID: UUID = UUID.fromString("0306caac-acd3-4b0a-9c58-000000000001")
        val PLAYER_1_ID: UUID = UUID.fromString("0306caac-acd3-4b0a-9c58-000000000002")
        val PLAYER_2_ID: UUID = UUID.fromString("0306caac-acd3-4b0a-9c58-000000000003")
        val MATCH_ID: UUID = UUID.fromString("0306caac-acd3-4b0a-9c58-000000000004")
    }

    @Test
    fun `сериализация и десериализация PersistenceData`() {
        val player1 = Player(PLAYER_1_ID, TEAM_1_ID, 22, "Михаил", "Сартаков")
        val player2 = Player(PLAYER_2_ID, TEAM_2_ID, 11, "Денис", "Братчиков")

        val team1 = Team(TEAM_1_ID, "НИИ ТУДА", listOf(PLAYER_1_ID))
        val team2 = Team(TEAM_2_ID, "НИИ СЮДА", listOf(PLAYER_2_ID))

        val timestamp = Instant.parse("2025-01-01T12:00:00Z")

        val match = Match(
            id = MATCH_ID,
            teams = listOf(team1, team2),
            events = mutableListOf(
                OnePlayerEvent(PLAYER_1_ID, TEAM_1_ID, timestamp, EventType.PULL),
                TwoPlayerEvent(PLAYER_1_ID, PLAYER_2_ID, TEAM_1_ID, TEAM_2_ID, timestamp.plusSeconds(10), EventType.GOAL),
            ),
        )

        val data = PersistenceData(
            version = 1,
            savedAt = timestamp,
            matches = listOf(match),
            teams = listOf(team1, team2),
            players = listOf(player1, player2),
        )

        val json = objectMapper.writeValueAsString(data)
        val restored = objectMapper.readValue(json, PersistenceData::class.java)

        assertEquals(data.version, restored.version)
        assertEquals(data.savedAt, restored.savedAt)
        assertEquals(data.matches.size, restored.matches.size)
        assertEquals(data.teams.size, restored.teams.size)
        assertEquals(data.players.size, restored.players.size)

        assertEquals(data.matches[0].id, restored.matches[0].id)
        assertEquals(data.matches[0].events.size, restored.matches[0].events.size)
    }

    @Test
    fun `сериализация всех типов событий`() {
        val timestamp = Instant.parse("2025-01-01T12:00:00Z")

        val events = listOf(
            OnePlayerEvent(PLAYER_1_ID, TEAM_1_ID, timestamp, EventType.PULL),
            OnePlayerEvent(PLAYER_1_ID, TEAM_1_ID, timestamp, EventType.BRICK),
            OnePlayerEvent(PLAYER_1_ID, TEAM_1_ID, timestamp, EventType.DROP),
            OnePlayerEvent(PLAYER_1_ID, TEAM_1_ID, timestamp, EventType.TURNOVER),
            TwoPlayerEvent(PLAYER_1_ID, PLAYER_2_ID, TEAM_1_ID, TEAM_2_ID, timestamp, EventType.PASS),
            TwoPlayerEvent(PLAYER_1_ID, PLAYER_2_ID, TEAM_1_ID, TEAM_2_ID, timestamp, EventType.GOAL),
            TwoPlayerEvent(PLAYER_1_ID, PLAYER_2_ID, TEAM_1_ID, TEAM_2_ID, timestamp, EventType.BLOCK_MARKER),
            TwoPlayerEvent(PLAYER_1_ID, PLAYER_2_ID, TEAM_1_ID, TEAM_2_ID, timestamp, EventType.BLOCK_FIELD),
            TwoPlayerEvent(PLAYER_1_ID, PLAYER_2_ID, TEAM_1_ID, TEAM_2_ID, timestamp, EventType.INTERCEPTION),
            TwoPlayerEvent(PLAYER_1_ID, PLAYER_2_ID, TEAM_1_ID, TEAM_2_ID, timestamp, EventType.CALLAHAN),
            TeamEvent(TEAM_1_ID, timestamp, EventType.TIMEOUT_START),
            TeamEvent(TEAM_1_ID, timestamp, EventType.TIMEOUT_END),
            SystemEvent(timestamp, EventType.HALFTIME_START),
            SystemEvent(timestamp, EventType.HALFTIME_END),
        )

        val match = Match(
            id = MATCH_ID,
            teams = listOf(Team(TEAM_1_ID, "Team1", emptyList()), Team(TEAM_2_ID, "Team2", emptyList())),
            events = events.toMutableList(),
        )

        val data = PersistenceData(matches = listOf(match))

        val json = objectMapper.writeValueAsString(data)
        val restored = objectMapper.readValue(json, PersistenceData::class.java)

        assertEquals(events.size, restored.matches[0].events.size)

        restored.matches[0].events.forEachIndexed { index, event ->
            assertEquals(events[index].type, event.type)
            assertEquals(events[index].realTimestamp, event.realTimestamp)
        }
    }
}
