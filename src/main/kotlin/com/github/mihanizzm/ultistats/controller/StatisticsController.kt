package com.github.mihanizzm.ultistats.controller

import com.github.mihanizzm.ultistats.model.statistics.MatchStatistics
import com.github.mihanizzm.ultistats.service.MatchService
import com.github.mihanizzm.ultistats.service.statistics.StatisticsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/matches/{matchId}/statistics")
@Tag(name = "Statistics", description = "Получение статистики матча")
class StatisticsController(
    private val statisticsService: StatisticsService,
    private val matchService: MatchService,
) {
    @GetMapping
    @Operation(summary = "Получить статистику матча")
    fun getStatistics(@PathVariable matchId: UUID): ResponseEntity<MatchStatistics> {
        if (matchService.get(matchId) == null) {
            return ResponseEntity.notFound().build()
        }
        val statistics = statisticsService.recalculateMatchStatistics(matchId)
        return ResponseEntity.ok(statistics)
    }
}
