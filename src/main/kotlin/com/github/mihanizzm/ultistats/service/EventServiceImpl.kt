package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.events.Event
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("unused")
class EventServiceImpl(
    private val matchService: MatchService,
) : EventService {
    /**
     * Создать новое событие.
     */
    override fun create(event: Event, matchId: UUID) {
        val match = matchService.getOrThrow(matchId)
        match.events.add(event)
    }

    /**
     * Изменить существующее событие по выбранному индексу.
     */
    override fun edit(index: Int, event: Event, matchId: UUID) {
        val match = matchService.getOrThrow(matchId)
        checkEventExistence(index, match)
        match.events[index] = event
    }

    /**
     * Удалить выбранное событие в матче по индексу.
     */
    override fun remove(index: Int, matchId: UUID) {
        val match = matchService.getOrThrow(matchId)
        checkEventExistence(index, match)
        match.events.removeAt(index)
    }

    /**
     * Получить все события выбранного матча.
     */
    override fun getAllEventsOfMatch(matchId: UUID): List<Event> {
        val match = matchService.getOrThrow(matchId)
        return match.events
    }

    private fun checkEventExistence(index: Int, match: Match) {
        if (index >= match.events.size) {
            throw IllegalArgumentException("Index $index is out of bounds for events of size ${match.events.size}")
        }
    }
}