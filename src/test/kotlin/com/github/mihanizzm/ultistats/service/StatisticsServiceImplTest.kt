package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.MatchAbstractTest
import com.github.mihanizzm.ultistats.model.events.BlockFieldEvent
import com.github.mihanizzm.ultistats.model.events.BlockMarkerEvent
import com.github.mihanizzm.ultistats.model.events.BrickEvent
import com.github.mihanizzm.ultistats.model.events.CallahanEvent
import com.github.mihanizzm.ultistats.model.events.DropEvent
import com.github.mihanizzm.ultistats.model.events.GoalEvent
import com.github.mihanizzm.ultistats.model.events.InterceptionEvent
import com.github.mihanizzm.ultistats.model.events.PassEvent
import com.github.mihanizzm.ultistats.model.events.PullEvent
import com.github.mihanizzm.ultistats.model.events.TurnoverEvent
import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import com.github.mihanizzm.ultistats.model.statistics.PlayerStatistics
import com.github.mihanizzm.ultistats.model.statistics.TeamStatistics
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@Suppress("NonAsciiCharacters")
class StatisticsServiceImplTest : MatchAbstractTest() {
    @BeforeEach
    fun setup() {
        teamService.create(TEAM_1)
        teamService.create(TEAM_2)
        matchService.create(MATCH)
        MATCH.events.clear()
    }

    @Test
    fun `Создание пустой статистики`() {
        val teamIds = listOf(TEAM_1.id, TEAM_2.id)
        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) },
        )

        val actual = statisticsService.emptyStatistics(teamIds)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика перебития в поле записана`() {
        MATCH.events.add(
            BlockFieldEvent(UUIDS[2], UUIDS[7], UUIDS[0], UUIDS[1], Instant.now()),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) }
                .map {
                    when (it.playerId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(dropsOnField = 1))
                        UUIDS[7] -> it.copy(defense = it.defense.copy(blocksAsFieldPlayer = 1))
                        else -> it
                    }
                },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) }
                .map {
                    when (it.teamId) {
                        UUIDS[0] -> it.copy(attack = it.attack.copy(allPasses = 1))
                        UUIDS[1] -> it.copy(defense = it.defense.copy(blocksAsFieldPlayer = 1))
                        else -> it
                    }
                },
        )

        val actual = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика кэллахана записана`() {
        MATCH.events.add(
            CallahanEvent(UUIDS[2], UUIDS[7], UUIDS[0], UUIDS[1], Instant.now()),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) }
                .map {
                    when (it.playerId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(callahanDrops = 1))
                        UUIDS[7] -> it.copy(
                            defense = it.defense.copy(callahans = 1),
                            attack = it.attack.copy(goals = 1)
                        )
                        else -> it
                    }
                },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) }
                .map {
                    when (it.teamId) {
                        UUIDS[0] -> it.copy(attack = it.attack.copy(allPasses = 1))
                        UUIDS[1] -> it.copy(
                            defense = it.defense.copy(callahans = 1),
                            attack = it.attack.copy(score = 1)
                        )
                        else -> it
                    }
                },
        )

        val actual = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика перебития на маркере записана`() {
        MATCH.events.add(
            BlockMarkerEvent(UUIDS[2], UUIDS[7], UUIDS[0], UUIDS[1], Instant.now()),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) }
                .map {
                    when (it.playerId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(dropsOnMarker = 1))
                        UUIDS[7] -> it.copy(defense = it.defense.copy(blocksAsMarker = 1))
                        else -> it
                    }
                },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) }
                .map {
                    when (it.teamId) {
                        UUIDS[0] -> it.copy(attack = it.attack.copy(allPasses = 1))
                        UUIDS[1] -> it.copy(defense = it.defense.copy(blocksAsMarker = 1))
                        else -> it
                    }
                },
        )

        val actual = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика перехвата записана`() {
        MATCH.events.add(
            InterceptionEvent(UUIDS[2], UUIDS[7], UUIDS[0], UUIDS[1], Instant.now()),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) }
                .map {
                    when (it.playerId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(dropsOnField = 1))
                        UUIDS[7] -> it.copy(defense = it.defense.copy(interceptions = 1))
                        else -> it
                    }
                },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) }
                .map {
                    when (it.teamId) {
                        UUIDS[0] -> it.copy(attack = it.attack.copy(allPasses = 1))
                        UUIDS[1] -> it.copy(defense = it.defense.copy(interceptions = 1))
                        else -> it
                    }
                },
        )

        val actual = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика гола записана`() {
        MATCH.events.add(
            GoalEvent(UUIDS[2], UUIDS[3], UUIDS[0], UUIDS[0], Instant.now()),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) }
                .map {
                    when (it.playerId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(
                            assists = 1,
                            passes = 1,
                        ))
                        UUIDS[3] -> it.copy(attack = it.attack.copy(
                            goals = 1,
                            catches = 1,
                        ))
                        else -> it
                    }
                },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) }
                .map {
                    when (it.teamId) {
                        UUIDS[0] -> it.copy(attack = it.attack.copy(
                            allPasses = 1,
                            completePasses = 1,
                            score = 1,
                        ))
                        else -> it
                    }
                },
        )

        val actual = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика паса записана`() {
        MATCH.events.add(
            PassEvent(UUIDS[2], UUIDS[3], UUIDS[0], UUIDS[0], Instant.now()),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) }
                .map {
                    when (it.playerId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(passes = 1))
                        UUIDS[3] -> it.copy(attack = it.attack.copy(catches = 1))
                        else -> it
                    }
                },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) }
                .map {
                    when (it.teamId) {
                        UUIDS[0] -> it.copy(attack = it.attack.copy(
                            allPasses = 1,
                            completePasses = 1,
                        ))
                        else -> it
                    }
                },
        )

        val actual = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика брика записана`() {
        MATCH.events.add(
            BrickEvent(UUIDS[2], UUIDS[0], Instant.now()),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) }
                .map {
                    when (it.playerId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(bricks = 1))
                        else -> it
                    }
                },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) }
                .map {
                    when (it.teamId) {
                        UUIDS[0] -> it.copy(attack = it.attack.copy(bricks = 1))
                        else -> it
                    }
                },
        )

        val actual = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика пулла записана`() {
        MATCH.events.add(
            PullEvent(UUIDS[2], UUIDS[0], Instant.now()),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) }
                .map {
                    when (it.playerId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(pulls = 1))
                        else -> it
                    }
                },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) }
                .map {
                    when (it.teamId) {
                        UUIDS[0] -> it.copy(attack = it.attack.copy(pulls = 1))
                        else -> it
                    }
                },
        )

        val actual = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика перехода владения записана`() {
        MATCH.events.add(
            TurnoverEvent(UUIDS[2], UUIDS[0], Instant.now()),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) }
                .map {
                    when (it.playerId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(discPossessions = 1))
                        else -> it
                    }
                },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) }
                .map {
                    when (it.teamId) {
                        UUIDS[0] -> it.copy(attack = it.attack.copy(possessions = 1))
                        else -> it
                    }
                },
        )

        val actual = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика дропа записана`() {
        MATCH.events.add(
            DropEvent(UUIDS[2], UUIDS[0], Instant.now()),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .mapNotNull { it.id }
                .map { PlayerStatistics(it) }
                .map {
                    when (it.playerId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(drops = 1))
                        else -> it
                    }
                },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) }
                .map {
                    when (it.teamId) {
                        UUIDS[0] -> it.copy(attack = it.attack.copy(allPasses = 1))
                        else -> it
                    }
                },
        )

        val actual = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(actual).isEqualTo(expectedStats)
    }
}
