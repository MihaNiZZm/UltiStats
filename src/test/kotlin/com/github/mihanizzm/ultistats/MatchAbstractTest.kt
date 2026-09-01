package com.github.mihanizzm.ultistats

import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.statistics.StatisticsService
import com.github.mihanizzm.ultistats.service.TeamService
import com.github.mihanizzm.ultistats.service.TeamPlayerService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.util.UUID

@SpringBootTest
@Suppress("unused")
abstract class MatchAbstractTest {
    @Autowired
    lateinit var statisticsService: StatisticsService

    @Autowired
    lateinit var matchService: MatchService

    @Autowired
    lateinit var teamService: TeamService

    @Autowired
    lateinit var eventService: EventService

    @Autowired
    lateinit var playerService: PlayerService

    @Autowired
    lateinit var teamPlayerService: TeamPlayerService

    companion object {
        const val START_DATE = "2025-11-23T12:00:00Z"
        const val END_DATE = "2025-11-23T12:30:00Z"

        val UUIDS = listOf(
            UUID.fromString("0306caac-acd3-4b0a-9c58-000000000000"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-000000000001"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-000000000002"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-000000000003"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-000000000004"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-000000000005"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-000000000006"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-000000000007"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-000000000008"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-000000000009"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-00000000000a"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-00000000000b"),
            UUID.fromString("0306caac-acd3-4b0a-9c58-00000000000c"),
        )

        val PLAYERS_1 = listOf(
            Player(
                UUIDS[2],
                "Михаил",
                "Сартаков",
            ),
            Player(
                UUIDS[3],
                "Николай",
                "Вихорев",
            ),
            Player(
                UUIDS[4],
                "Денис",
                "Братчиков",
            ),
            Player(
                UUIDS[5],
                "Валерия",
                "Сердюк",
            ),
            Player(
                UUIDS[6],
                "Олег",
                "Судоплатов",
            ),
        )

        val PLAYERS_2 = listOf(
            Player(
                UUIDS[7],
                "Алексей",
                "Иванов",
            ),
            Player(
                UUIDS[8],
                "Иван",
                "Петров",
            ),
            Player(
                UUIDS[9],
                "Сергей",
                "Тришкин",
            ),
            Player(
                UUIDS[10],
                "Ксения",
                "Важева",
            ),
            Player(
                UUIDS[11],
                "Андрей",
                "Туркин",
            ),
        )

        val TEAM_1 = Team(
            UUIDS[0],
            "НИИ ТУДА",
        )
        val TEAM_2 = Team(
            UUIDS[1],
            "НИИ СЮДА",
        )

        val MATCH = Match(
            UUIDS[12],
            listOf(TEAM_1.id, TEAM_2.id),
            mutableListOf<Event>(),
        )
    }

    @BeforeEach
    fun setupTestData() {
        matchService.getAll().forEach { matchService.delete(it.id) }
        teamService.getAll().forEach { teamService.delete(it.id) }
        playerService.getAll().forEach { playerService.delete(it.id) }

        teamService.create(TEAM_1)
        teamService.create(TEAM_2)

        PLAYERS_1.forEach { playerService.create(it) }
        PLAYERS_2.forEach { playerService.create(it) }
        PLAYERS_1.forEachIndexed { index, player -> teamPlayerService.add(TEAM_1.id, player.id, index + 1) }
        PLAYERS_2.forEachIndexed { index, player -> teamPlayerService.add(TEAM_2.id, player.id, index + 1) }

        matchService.create(MATCH)
        matchService.startMatch(MATCH.id, Instant.parse(START_DATE))
    }

    protected fun recalculateTestMatchStatistics(): MatchStatistics {
        MATCH.events.toList().forEach { eventService.create(it, MATCH.id) }
        MATCH.events.clear()
        return statisticsService.recalculateMatchStatistics(matchService.getOrThrow(MATCH.id))
    }
}
