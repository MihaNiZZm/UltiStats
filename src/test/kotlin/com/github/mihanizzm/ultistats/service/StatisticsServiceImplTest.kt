package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.MatchAbstractTest
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.OnePlayerEvent
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import com.github.mihanizzm.ultistats.model.statistics.PlayerStatistics
import com.github.mihanizzm.ultistats.model.statistics.TeamStatistics
import com.github.mihanizzm.ultistats.service.statistics.IncrementalStatisticsAggregator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@Suppress("NonAsciiCharacters")
class StatisticsServiceImplTest : MatchAbstractTest() {
    @Autowired
    lateinit var incrementalStatisticsAggregator: IncrementalStatisticsAggregator

    @BeforeEach
    fun setup() {
        MATCH.events.clear()
        matchService.update(MATCH)
    }

    @Test
    fun `Создание пустой статистики`() {
        val teamIds = listOf(TEAM_1.id, TEAM_2.id)
        val expectedStats = MatchStatistics(
            playerStatistics = (PLAYERS_1 + PLAYERS_2).map { PlayerStatistics(it.id) },
            teamStatistics = listOf(TEAM_1, TEAM_2)
                .map { TeamStatistics(it.id) },
        )

        val actual = statisticsService.emptyStatistics(teamIds)

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Незавершенный пас записывается бросающему`() {
        MATCH.events.add(
            OnePlayerEvent(UUIDS[2], Instant.now(), EventType.INCOMPLETE_PASS),
        )

        val actual = recalculateIncrementalStatistics()

        assertThat(
            actual.playerStatistics.first { it.participantId == UUIDS[2] }.attack.incompletePasses,
        ).isEqualTo(1)
        assertThat(
            actual.teamStatistics.first { it.teamId == UUIDS[0] }.attack.allPasses,
        ).isEqualTo(1)
    }

    @Test
    fun `Общий блок учитывается защитнику и команде`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[7], Instant.now(), EventType.BLOCK),
        )

        val actual = recalculateIncrementalStatistics()

        assertThat(
            actual.playerStatistics.first { it.participantId == UUIDS[7] }.defense.blocks,
        ).isEqualTo(1)
        assertThat(
            actual.teamStatistics.first { it.teamId == UUIDS[1] }.defense.blocks,
        ).isEqualTo(1)
        assertThat(
            actual.teamStatistics.first { it.teamId == UUIDS[0] }.attack.allPasses,
        ).isEqualTo(1)
    }

    @Test
    fun `Уточненный полевой блок не дублируется как общий блок`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[7], Instant.now(), EventType.BLOCK_FIELD),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = matchParticipantStatistics()
                .map {
                    when (it.participantId) {
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

        val actual = recalculateIncrementalStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика кэллахана записана`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[7], Instant.now(), EventType.CALLAHAN),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = matchParticipantStatistics()
                .map {
                    when (it.participantId) {
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

        val actual = recalculateIncrementalStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Уточненный блок маркера не дублируется как общий блок`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[7], Instant.now(), EventType.BLOCK_MARKER),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = matchParticipantStatistics()
                .map {
                    when (it.participantId) {
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

        val actual = recalculateIncrementalStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика перехвата записана`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[7], Instant.now(), EventType.INTERCEPTION),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = matchParticipantStatistics()
                .map {
                    when (it.participantId) {
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

        val actual = recalculateIncrementalStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика гола записана`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[3], Instant.now(), EventType.GOAL),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = matchParticipantStatistics()
                .map {
                    when (it.participantId) {
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

        val actual = recalculateIncrementalStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика паса записана`() {
        MATCH.events.add(
            TwoPlayerEvent(UUIDS[2], UUIDS[3], Instant.now(), EventType.PASS),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = matchParticipantStatistics()
                .map {
                    when (it.participantId) {
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

        val actual = recalculateIncrementalStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика брика записана`() {
        MATCH.events.add(
            OnePlayerEvent(UUIDS[2], Instant.now(), EventType.BRICK),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = matchParticipantStatistics()
                .map {
                    when (it.participantId) {
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

        val actual = recalculateIncrementalStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика пулла записана`() {
        MATCH.events.add(
            OnePlayerEvent(UUIDS[2], Instant.now(), EventType.PULL),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = matchParticipantStatistics()
                .map {
                    when (it.participantId) {
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

        val actual = recalculateIncrementalStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика перехода владения записана`() {
        MATCH.events.add(
            OnePlayerEvent(UUIDS[2], Instant.now(), EventType.PICKUP),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = matchParticipantStatistics()
                .map {
                    when (it.participantId) {
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

        val actual = recalculateIncrementalStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    @Test
    fun `Статистика дропа записана`() {
        MATCH.events.add(
            OnePlayerEvent(UUIDS[2], Instant.now(), EventType.INCOMPLETE_PASS),
        )

        val expectedStats = MatchStatistics(
            playerStatistics = matchParticipantStatistics()
                .map {
                    when (it.participantId) {
                        UUIDS[2] -> it.copy(attack = it.attack.copy(incompletePasses = 1))
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

        val actual = recalculateIncrementalStatistics()

        assertThat(actual).isEqualTo(expectedStats)
    }

    private fun matchParticipantStatistics(): List<PlayerStatistics> =
        matchService.getOrThrow(MATCH.id).participantsByTeam.values.flatten()
            .map { PlayerStatistics(it.participantId) }

    private fun recalculateIncrementalStatistics(): MatchStatistics {
        val match = matchService.getOrThrow(MATCH.id)
        val initial = MatchStatistics(
            playerStatistics = matchParticipantStatistics(),
            teamStatistics = match.teamIds.map(::TeamStatistics),
        )
        val teamByParticipantId = match.participantsByTeam.flatMap { (teamId, participants) ->
            participants.map { it.participantId to teamId }
        }.toMap()
        val events = MATCH.events.toList()
        MATCH.events.clear()
        return incrementalStatisticsAggregator.aggregate(initial, events, teamByParticipantId)
    }
}
