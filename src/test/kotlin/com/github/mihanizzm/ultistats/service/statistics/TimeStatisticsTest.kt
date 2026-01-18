package com.github.mihanizzm.ultistats.service.statistics

import com.github.mihanizzm.ultistats.MatchAbstractTest
import com.github.mihanizzm.ultistats.model.events.DropEvent
import com.github.mihanizzm.ultistats.model.events.GoalEvent
import com.github.mihanizzm.ultistats.model.events.HalftimeEndEvent
import com.github.mihanizzm.ultistats.model.events.HalftimeStartEvent
import com.github.mihanizzm.ultistats.model.events.PassEvent
import com.github.mihanizzm.ultistats.model.events.PullEvent
import com.github.mihanizzm.ultistats.model.events.TimeoutEndEvent
import com.github.mihanizzm.ultistats.model.events.TimeoutStartEvent
import com.github.mihanizzm.ultistats.model.events.TurnoverEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

@Suppress("NonAsciiCharacters")
class TimeStatisticsTest : MatchAbstractTest() {
    private fun ts(sec: Long): Instant = Instant.parse(START_DATE).plusSeconds(sec)

    @BeforeEach
    fun setup() {
        teamService.create(TEAM_1)
        teamService.create(TEAM_2)
        matchService.create(MATCH)
        MATCH.events.clear()
    }

    @Test
    fun `Владение только у атакующей команды, игроки получают свое время`() {
        // Пулл (TEAM_1) -> Подбор (TEAM_2) -> Пас (TEAM_2) -> Гол (TEAM_2)
        MATCH.events.add(PullEvent(UUIDS[2], UUIDS[0], ts(0)))
        MATCH.events.add(TurnoverEvent(UUIDS[7], UUIDS[1], ts(10)))
        MATCH.events.add(PassEvent(UUIDS[7], UUIDS[8], UUIDS[1], UUIDS[1], ts(25)))
        MATCH.events.add(GoalEvent(UUIDS[8], UUIDS[9], UUIDS[1], UUIDS[1], ts(40)))

        val stats = statisticsService.recalculateMatchStatistics(MATCH.id)

        val team1 = stats.teamStatistics.first { it.teamId == UUIDS[0] }.time
        val team2 = stats.teamStatistics.first { it.teamId == UUIDS[1] }.time
        assertThat(team1.totalPossessionTime).isEqualTo(Duration.ofSeconds(0))
        assertThat(team2.totalPossessionTime).isEqualTo(Duration.ofSeconds(30)) // 10..40

        val p7 = stats.playerStatistics.first { it.playerId == UUIDS[7] }.time
        val p8 = stats.playerStatistics.first { it.playerId == UUIDS[8] }.time
        assertThat(p7.totalPossessionTime).isEqualTo(Duration.ofSeconds(15)) // 10..25
        assertThat(p8.totalPossessionTime).isEqualTo(Duration.ofSeconds(15)) // 25..40
    }

    @Test
    fun `Таймаут учитывается в общих и командных таймингах`() {
        // Пулл -> Подбор -> Таймаут -> Конец таймаута -> Пас -> Гол
        MATCH.events.add(PullEvent(UUIDS[2], UUIDS[0], ts(0)))
        MATCH.events.add(TurnoverEvent(UUIDS[7], UUIDS[1], ts(10)))
        MATCH.events.add(TimeoutStartEvent(UUIDS[1], ts(15)))
        MATCH.events.add(TimeoutEndEvent(UUIDS[1], ts(75))) // 60 сек таймаута
        MATCH.events.add(PassEvent(UUIDS[7], UUIDS[8], UUIDS[1], UUIDS[1], ts(90)))
        MATCH.events.add(GoalEvent(UUIDS[8], UUIDS[9], UUIDS[1], UUIDS[1], ts(100)))

        val stats = statisticsService.recalculateMatchStatistics(MATCH.id)

        // Матчевая статистика по времени
        assertThat(stats.timeStatistics.timeSpentOnTimeouts).isEqualTo(Duration.ofSeconds(60))

        // Командная статистика по времени таймаутов
        val team2 = stats.teamStatistics.first { it.teamId == UUIDS[1] }.time
        assertThat(team2.totalTimeSpentOnTimeouts).isEqualTo(Duration.ofSeconds(60))

        // Время владения команды не включает таймаут
        assertThat(team2.totalPossessionTime).isEqualTo(Duration.ofSeconds(30)) // 10..15 и 75..100
    }

    @Test
    fun `Время между очками увеличивается у команды со вторым пуллом`() {
        // Первый поинт
        MATCH.events.add(PullEvent(UUIDS[2], UUIDS[0], ts(0)))
        MATCH.events.add(TurnoverEvent(UUIDS[7], UUIDS[1], ts(10)))
        MATCH.events.add(PassEvent(UUIDS[7], UUIDS[8], UUIDS[1], UUIDS[1], ts(20)))
        MATCH.events.add(GoalEvent(UUIDS[8], UUIDS[9], UUIDS[1], UUIDS[1], ts(30)))
        // Второй пулл делает команда забившая гол (TEAM_2)
        MATCH.events.add(PullEvent(UUIDS[7], UUIDS[1], ts(50))) // между очками 20 сек

        val stats = statisticsService.recalculateMatchStatistics(MATCH.id)
        val team1 = stats.teamStatistics.first { it.teamId == UUIDS[0] }.time
        val team2 = stats.teamStatistics.first { it.teamId == UUIDS[1] }.time

        assertThat(team1.totalTimeBetweenPoints).isEqualTo(Duration.ZERO)
        assertThat(team2.totalTimeBetweenPoints).isEqualTo(Duration.ofSeconds(20)) // 30..50
    }

    @Test
    fun `Халфтайм учитывается и не попадает во время между очками`() {
        // Поинт до халфтайма
        MATCH.events.add(PullEvent(UUIDS[2], UUIDS[0], ts(0)))
        MATCH.events.add(TurnoverEvent(UUIDS[7], UUIDS[1], ts(10)))
        MATCH.events.add(PassEvent(UUIDS[7], UUIDS[8], UUIDS[1], UUIDS[1], ts(20)))
        MATCH.events.add(GoalEvent(UUIDS[8], UUIDS[9], UUIDS[1], UUIDS[1], ts(30)))
        // Халфтайм 60 сек
        MATCH.events.add(HalftimeStartEvent(ts(31)))
        MATCH.events.add(HalftimeEndEvent(ts(91)))
        // Новый поинт после халфтайма
        MATCH.events.add(PullEvent(UUIDS[7], UUIDS[1], ts(100))) // между очками после халфтайма: 91..100 = 9 сек
        MATCH.events.add(TurnoverEvent(UUIDS[3], UUIDS[0], ts(110)))
        MATCH.events.add(PassEvent(UUIDS[3], UUIDS[2], UUIDS[0], UUIDS[0], ts(120)))
        MATCH.events.add(GoalEvent(UUIDS[2], UUIDS[5], UUIDS[0], UUIDS[0], ts(130)))

        val stats = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(stats.timeStatistics.timeSpentOnHalftime).isEqualTo(Duration.ofSeconds(60))

        val team2 = stats.teamStatistics.first { it.teamId == UUIDS[1] }.time
        assertThat(team2.totalTimeBetweenPoints).isEqualTo(Duration.ofSeconds(9)) // только после HALFTIME_END
    }

    @Test
    fun `Чистое время исключает между поинтами, таймауты и халфтайм`() {
        // Поинт 1 с таймаутом
        MATCH.events.add(PullEvent(UUIDS[2], UUIDS[0], ts(0)))
        MATCH.events.add(TurnoverEvent(UUIDS[7], UUIDS[1], ts(10)))
        MATCH.events.add(TimeoutStartEvent(UUIDS[1], ts(20)))
        MATCH.events.add(TimeoutEndEvent(UUIDS[1], ts(80))) // 60
        MATCH.events.add(PassEvent(UUIDS[7], UUIDS[8], UUIDS[1], UUIDS[1], ts(90)))
        MATCH.events.add(GoalEvent(UUIDS[8], UUIDS[9], UUIDS[1], UUIDS[1], ts(100)))
        // Между поинтами
        MATCH.events.add(PullEvent(UUIDS[7], UUIDS[1], ts(130))) // 30
        // Поинт 2 без остановок
        MATCH.events.add(TurnoverEvent(UUIDS[3], UUIDS[0], ts(140)))
        MATCH.events.add(PassEvent(UUIDS[3], UUIDS[2], UUIDS[0], UUIDS[0], ts(150)))
        MATCH.events.add(GoalEvent(UUIDS[2], UUIDS[5], UUIDS[0], UUIDS[0], ts(160)))

        val stats = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(stats.timeStatistics.timeSpentOnTimeouts).isEqualTo(Duration.ofSeconds(60))
        assertThat(stats.timeStatistics.timeSpentBetweenPoints).isEqualTo(Duration.ofSeconds(30))
        assertThat(stats.timeStatistics.timeSpentOnHalftime).isEqualTo(Duration.ZERO)

        val expectedPure = Duration.ofSeconds(160) // суммарное время между всеми событиями
            .minus(Duration.ofSeconds(30)) // между поинтами
            .minus(Duration.ofSeconds(60)) // таймауты
            .minus(Duration.ZERO) // халфтайм
        assertThat(stats.timeStatistics.pureGameTime).isEqualTo(expectedPure)
    }

    @Test
    fun `Между поинтами не включает халфтайм (минимальная последовательность)`() {
        // Поинт завершен голом
        MATCH.events.add(PullEvent(UUIDS[2], UUIDS[0], ts(0)))
        MATCH.events.add(TurnoverEvent(UUIDS[7], UUIDS[1], ts(10)))
        MATCH.events.add(PassEvent(UUIDS[7], UUIDS[8], UUIDS[1], UUIDS[1], ts(20)))
        MATCH.events.add(GoalEvent(UUIDS[8], UUIDS[9], UUIDS[1], UUIDS[1], ts(30)))
        // Халфтайм 60 сек сразу после поинта
        MATCH.events.add(HalftimeStartEvent(ts(31)))
        MATCH.events.add(HalftimeEndEvent(ts(91)))
        // Следующий пулл (время между поинтами должно считаться только после HALFTIME_END)
        MATCH.events.add(PullEvent(UUIDS[7], UUIDS[1], ts(95))) // 91..95 = 4 сек

        val stats = statisticsService.recalculateMatchStatistics(MATCH.id)

        assertThat(stats.timeStatistics.timeSpentOnHalftime).isEqualTo(Duration.ofSeconds(60))
        val team2 = stats.teamStatistics.first { it.teamId == UUIDS[1] }.time
        assertThat(team2.totalTimeBetweenPoints).isEqualTo(Duration.ofSeconds(4))
    }

    @Test
    fun `Время игроков в длинной цепочке пасов считается корректно`() {
        // Пулл -> Подбор -> несколько пасов -> Гол
        MATCH.events.add(PullEvent(UUIDS[2], UUIDS[0], ts(0)))
        MATCH.events.add(TurnoverEvent(UUIDS[7], UUIDS[1], ts(5)))
        MATCH.events.add(PassEvent(UUIDS[7], UUIDS[8], UUIDS[1], UUIDS[1], ts(15)))
        MATCH.events.add(PassEvent(UUIDS[8], UUIDS[9], UUIDS[1], UUIDS[1], ts(25)))
        MATCH.events.add(PassEvent(UUIDS[9], UUIDS[10], UUIDS[1], UUIDS[1], ts(35)))
        MATCH.events.add(GoalEvent(UUIDS[10], UUIDS[11], UUIDS[1], UUIDS[1], ts(50)))

        val stats = statisticsService.recalculateMatchStatistics(MATCH.id)
        fun playerTime(idIdx: Int) = stats.playerStatistics.first { it.playerId == UUIDS[idIdx] }.time.totalPossessionTime

        assertThat(playerTime(7)).isEqualTo(Duration.ofSeconds(10)) // 5..15
        assertThat(playerTime(8)).isEqualTo(Duration.ofSeconds(10)) // 15..25
        assertThat(playerTime(9)).isEqualTo(Duration.ofSeconds(10)) // 25..35
        assertThat(playerTime(10)).isEqualTo(Duration.ofSeconds(15)) // 35..50
    }

    @Test
    fun `Время владения не начисляется между дропом и подбором другой командой`() {
        // Пулл -> Подбор (TEAM_2) -> Пас -> Дроп -> Подбор (TEAM_1) -> Пас -> Гол
        MATCH.events.add(PullEvent(UUIDS[2], UUIDS[0], ts(0)))
        MATCH.events.add(TurnoverEvent(UUIDS[7], UUIDS[1], ts(10)))
        MATCH.events.add(PassEvent(UUIDS[7], UUIDS[8], UUIDS[1], UUIDS[1], ts(20)))
        MATCH.events.add(DropEvent(UUIDS[8], UUIDS[1], ts(25))) // владение никому до следующего подбора
        MATCH.events.add(TurnoverEvent(UUIDS[3], UUIDS[0], ts(40)))
        MATCH.events.add(PassEvent(UUIDS[3], UUIDS[2], UUIDS[0], UUIDS[0], ts(50)))
        MATCH.events.add(GoalEvent(UUIDS[2], UUIDS[5], UUIDS[0], UUIDS[0], ts(60)))

        val stats = statisticsService.recalculateMatchStatistics(MATCH.id)
        val team1 = stats.teamStatistics.first { it.teamId == UUIDS[0] }.time
        val team2 = stats.teamStatistics.first { it.teamId == UUIDS[1] }.time

        assertThat(team2.totalPossessionTime).isEqualTo(Duration.ofSeconds(15)) // 10..25
        assertThat(team1.totalPossessionTime).isEqualTo(Duration.ofSeconds(20)) // 40..60
    }

    @Test
    fun `Пулл не дает владение до подбора диска другой командой`() {
        // Пулл (TEAM_1) -> длительная пауза -> Подбор (TEAM_2) -> Пас -> Гол
        MATCH.events.add(PullEvent(UUIDS[2], UUIDS[0], ts(0)))
        MATCH.events.add(TurnoverEvent(UUIDS[7], UUIDS[1], ts(30)))
        MATCH.events.add(PassEvent(UUIDS[7], UUIDS[8], UUIDS[1], UUIDS[1], ts(50)))
        MATCH.events.add(GoalEvent(UUIDS[8], UUIDS[9], UUIDS[1], UUIDS[1], ts(60)))

        val stats = statisticsService.recalculateMatchStatistics(MATCH.id)
        val team1 = stats.teamStatistics.first { it.teamId == UUIDS[0] }.time
        val team2 = stats.teamStatistics.first { it.teamId == UUIDS[1] }.time

        assertThat(team1.totalPossessionTime).isEqualTo(Duration.ZERO) // после пулла до подбора владения нет
        assertThat(team2.totalPossessionTime).isEqualTo(Duration.ofSeconds(30)) // 30..60
    }
}


