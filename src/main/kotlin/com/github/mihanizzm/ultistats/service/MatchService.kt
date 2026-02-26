package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.Match
import java.time.Instant
import java.util.UUID

interface MatchService {
    fun get(matchId: UUID): Match?

    fun getOrThrow(matchId: UUID): Match

    fun create(match: Match)

    fun delete(matchId: UUID)

    fun getAll(): List<Match>

    /**
     * Пересчитать владельца диска на основе списка событий матча.
     */
    fun recalculateDiskHolder(matchId: UUID)

    /**
     * Начать матч. Устанавливает startedAt в переданное время.
     * @param timestamp время начала матча, переданное клиентом
     * @return true если матч успешно начат, false если матч уже начат
     */
    fun startMatch(matchId: UUID, timestamp: Instant): Boolean

    /**
     * Завершить матч. Устанавливает endedAt в переданное время.
     * @param timestamp время окончания матча, переданное клиентом
     * @return true если матч успешно завершён, false если матч не начат или уже завершён
     */
    fun endMatch(matchId: UUID, timestamp: Instant): Boolean
}