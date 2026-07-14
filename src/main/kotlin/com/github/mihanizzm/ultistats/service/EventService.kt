package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.events.StoredEvent
import java.util.UUID

interface EventService {
    fun create(event: Event, matchId: UUID): StoredEvent
    fun get(eventId: UUID, matchId: UUID): StoredEvent?
    fun update(eventId: UUID, event: Event, matchId: UUID): StoredEvent
    fun remove(eventId: UUID, matchId: UUID): Boolean
    fun getAllEventsOfMatch(matchId: UUID): List<StoredEvent>
}
