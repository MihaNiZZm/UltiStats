package com.github.mihanizzm.ultistats

import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.Player
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.service.EventService
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.PlayerService
import com.github.mihanizzm.ultistats.service.statistics.StatisticsService
import com.github.mihanizzm.ultistats.service.TeamService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
                UUIDS[0],
                22,
                "Михаил",
                "Сартаков",
            ),
            Player(
                UUIDS[3],
                UUIDS[0],
                71,
                "Николай",
                "Вихорев",
            ),
            Player(
                UUIDS[4],
                UUIDS[0],
                11,
                "Денис",
                "Братчиков",
            ),
            Player(
                UUIDS[5],
                UUIDS[0],
                73,
                "Валерия",
                "Сердюк",
            ),
            Player(
                UUIDS[6],
                UUIDS[0],
                69,
                "Олег",
                "Судоплатов",
            ),
        )

        val PLAYERS_2 = listOf(
            Player(
                UUIDS[7],
                UUIDS[1],
                1,
                "Алексей",
                "Иванов",
            ),
            Player(
                UUIDS[8],
                UUIDS[1],
                2,
                "Иван",
                "Петров",
            ),
            Player(
                UUIDS[9],
                UUIDS[1],
                5,
                "Сергей",
                "Тришкин",
            ),
            Player(
                UUIDS[10],
                UUIDS[1],
                3,
                "Ксения",
                "Важева",
            ),
            Player(
                UUIDS[11],
                UUIDS[1],
                4,
                "Андрей",
                "Туркин",
            ),
        )

        val TEAM_1 = Team(
            UUIDS[0],
            "НИИ ТУДА",
            PLAYERS_1.map { it.id },
        )
        val TEAM_2 = Team(
            UUIDS[1],
            "НИИ СЮДА",
            PLAYERS_2.map { it.id },
        )

        val MATCH = Match(
            UUIDS[12],
            listOf(TEAM_1.id, TEAM_2.id),
            mutableListOf<Event>(),
        ).apply { initTeamScores() }
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

        matchService.create(MATCH)
    }
}
