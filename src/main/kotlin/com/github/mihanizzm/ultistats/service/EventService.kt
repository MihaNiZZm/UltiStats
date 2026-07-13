package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.events.Event
import java.util.UUID

/**
 * Сервис управления событиями матча.
 */
interface EventService {
    /**
     * Создать новое событие.
     * @return ID созданного события
     */
    fun create(event: Event, matchId: UUID): UUID

    /**
     * Изменить существующее событие по выбранному индексу.
     * @return ID изменённого события
     */
    fun edit(index: Int, event: Event, matchId: UUID): UUID

    /**
     * Удалить выбранное событие в матче по индексу.
     * @return ID удалённого события
     */
    fun remove(index: Int, matchId: UUID): UUID

    /**
     * Получить все события выбранного матча.
     */
    fun getAllEventsOfMatch(matchId: UUID): List<Event>
}
