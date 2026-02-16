package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.events.Event
import java.util.UUID

/**
 * Сервис управления событиями матча.
 */
interface EventService {
    /**
     * Создать новое событие.
     * @return ID игрока, который сейчас владеет диском (или null)
     */
    fun create(event: Event, matchId: UUID): UUID?

    /**
     * Изменить существующее событие по выбранному индексу.
     * @return ID игрока, который сейчас владеет диском (или null)
     */
    fun edit(index: Int, event: Event, matchId: UUID): UUID?

    /**
     * Удалить выбранное событие в матче по индексу.
     * @return ID игрока, который сейчас владеет диском (или null)
     */
    fun remove(index: Int, matchId: UUID): UUID?

    /**
     * Получить все события выбранного матча.
     */
    fun getAllEventsOfMatch(matchId: UUID): List<Event>
}