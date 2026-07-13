package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.MatchAbstractTest
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
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
        MATCH.events.clear()
        matchService.update(MATCH)
    }

    private fun recalculateTestMatchStatistics(): MatchStatistics {
        MATCH.events.toList().forEach { eventService.create(it, MATCH.id) }
        MATCH.events.clear()
        return statisticsService.recalculateMatchStatistics(MATCH.id)
    }

    @Test
    fun `Создание пустой статистики`() {
        val teamIds = listOf(TEAM_1.id, TEAM_2.id)
        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) },
        )

        val actual = statisticsService.emptyStatistics(teamIds)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика перебития в поле записана`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[7], UUIDS[0], UUIDS[1], Instant.now(), EventType.BLOCK_FIELD),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) }
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

        val actual = recalculateTestMatchStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика кэллахана записана`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[7], UUIDS[0], UUIDS[1], Instant.now(), EventType.CALLAHAN),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) }
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

        val actual = recalculateTestMatchStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика перебития на маркере записана`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[7], UUIDS[0], UUIDS[1], Instant.now(), EventType.BLOCK_MARKER),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) }
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

        val actual = recalculateTestMatchStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика перехвата записана`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[7], UUIDS[0], UUIDS[1], Instant.now(), EventType.INTERCEPTION),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) }
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

        val actual = recalculateTestMatchStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика гола записана`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[3], UUIDS[0], UUIDS[0], Instant.now(), EventType.GOAL),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) }
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

        val actual = recalculateTestMatchStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика паса записана`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[3], UUIDS[0], UUIDS[0], Instant.now(), EventType.PASS),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) }
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

        val actual = recalculateTestMatchStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика брика записана`() {
        MATCH.events.add(
            OnePlayerEvent(UUIDS[2], UUIDS[0], Instant.now(), EventType.BRICK),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) }
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

        val actual = recalculateTestMatchStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика пулла записана`() {
        MATCH.events.add(
            OnePlayerEvent(UUIDS[2], UUIDS[0], Instant.now(), EventType.PULL),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) }
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

        val actual = recalculateTestMatchStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика перехода владения записана`() {
        MATCH.events.add(
            OnePlayerEvent(UUIDS[2], UUIDS[0], Instant.now(), EventType.TURNOVER),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) }
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

        val actual = recalculateTestMatchStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика дропа записана`() {
        MATCH.events.add(
            OnePlayerEvent(UUIDS[2], UUIDS[0], Instant.now(), EventType.DROP),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2)
                .map { PlayerStatistics(it.id) }
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

        val actual = recalculateTestMatchStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }
}
