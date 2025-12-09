package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.events.Event
import java.util.UUID

/**
 * Сервис управления событиями матча.
 */
interface EventService {
    /**
     * Создать новое событие.
     */
    fun create(event: Event, matchId: UUID)

    /**
     * Изменить существующее событие по выбранному индексу.
     */
    fun edit(index: Int, event: Event, matchId: UUID)

    /**
     * Удалить выбранное событие в матче по индексу.
     */
    fun remove(index: Int, matchId: UUID)

    /**
     * Получить все события выбранного матча.
     */
    fun getAllEventsOfMatch(matchId: UUID): List<Event>
}