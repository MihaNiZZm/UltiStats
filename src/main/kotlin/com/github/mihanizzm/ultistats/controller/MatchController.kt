package com.github.mihanizzm.ultistats.controller

import com.github.mihanizzm.ultistats.dto.common.PageResponse
import com.github.mihanizzm.ultistats.dto.common.SortParam
import com.github.mihanizzm.ultistats.dto.request.CreateMatchRequest
import com.github.mihanizzm.ultistats.dto.request.MatchFilterRequest
import com.github.mihanizzm.ultistats.dto.request.MatchTimestampRequest
import com.github.mihanizzm.ultistats.dto.response.MatchResponse
import com.github.mihanizzm.ultistats.facade.MatchFacade
import com.github.mihanizzm.ultistats.model.MatchStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/matches")
@Tag(name = "Matches", description = "Управление матчами")
class MatchController(
    private val matchFacade: MatchFacade,
) {
    @GetMapping
    @Operation(summary = "Получить матчи с пагинацией, фильтрацией и сортировкой")
    fun getAll(
        @Parameter(description = "Номер страницы (начиная с 0)")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Размер страницы")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Фильтр по ID команды-участника")
        @RequestParam(required = false) teamId: UUID?,
        @Parameter(description = "Фильтр по статусу матча (PLANNED, IN_PROGRESS, FINISHED)")
        @RequestParam(required = false) status: MatchStatus?,
        @Parameter(description = "Фильтр по дате начиная с (ISO 8601)")
        @RequestParam(required = false) dateFrom: Instant?,
        @Parameter(description = "Фильтр по дате до (ISO 8601)")
        @RequestParam(required = false) dateTo: Instant?,
        @Parameter(
            description = "Сортировка. Формат: field:direction. " +
                "Доступные поля: plannedStartTimestamp, startedAt, endedAt, status. " +
                "По умолчанию: plannedStartTimestamp:asc",
            example = "plannedStartTimestamp:asc"
        )
        @RequestParam(required = false) sort: String?,
    ): PageResponse<MatchResponse> {
        val filter = MatchFilterRequest(
            teamId = teamId,
            status = status,
            dateFrom = dateFrom,
            dateTo = dateTo,
        )
        val sortParam = sort?.let { SortParam.parse(it) } ?: MatchFacade.DEFAULT_SORT
        return matchFacade.getAllPaged(page, size, filter, sortParam)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить матч по ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<MatchResponse> =
        matchFacade.getById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    @Operation(summary = "Создать матч")
    fun create(@RequestBody request: CreateMatchRequest): ResponseEntity<MatchResponse> =
        matchFacade.create(request)
            ?.let { ResponseEntity.status(HttpStatus.CREATED).body(it) }
            ?: ResponseEntity.badRequest().build()

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить матч")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID): ResponseEntity<Unit> =
        if (matchFacade.delete(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    @PostMapping("/{id}/start")
    @Operation(summary = "Начать матч")
    fun startMatch(
        @PathVariable id: UUID,
        @RequestBody request: MatchTimestampRequest,
    ): ResponseEntity<MatchResponse> =
        matchFacade.startMatch(id, request)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.badRequest().build()

    @PostMapping("/{id}/end")
    @Operation(summary = "Завершить матч")
    fun endMatch(
        @PathVariable id: UUID,
        @RequestBody request: MatchTimestampRequest,
    ): ResponseEntity<MatchResponse> =
        matchFacade.endMatch(id, request)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.badRequest().build()
}
