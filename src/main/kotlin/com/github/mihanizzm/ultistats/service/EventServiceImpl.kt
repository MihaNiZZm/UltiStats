package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.EventEntity
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.events.StoredEvent
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataEventRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class EventServiceImpl(
    private val eventRepository: SpringDataEventRepository,
    private val matchService: MatchService,
) : EventService {
    @Transactional
    override fun create(event: Event, matchId: UUID): StoredEvent {
        val match = matchService.getOrThrow(matchId)
        require(match.startedAt == null || !event.occurredAt.isBefore(match.startedAt)) { "Event precedes match start" }
        require(match.endedAt == null || !event.occurredAt.isAfter(match.endedAt)) { "Event follows match end" }
        val lastInSequence = eventRepository.findFirstByMatchIdOrderBySequenceNumberDesc(matchId)
        val lastActive = eventRepository.findFirstByMatchIdAndDeletedAtIsNullOrderBySequenceNumberDesc(matchId)
        require(lastActive == null || !event.occurredAt.isBefore(lastActive.occurredAt)) { "Event precedes the previous event" }
        val entity = EventEntity.fromDomain(UUID.randomUUID(), matchId, (lastInSequence?.sequenceNumber ?: 0) + 1, event)
        eventRepository.save(entity)
        matchService.recalculateScore(matchId)
        return entity.toStored()
    }

    override fun get(eventId: UUID, matchId: UUID): StoredEvent? {
        matchService.getOrThrow(matchId)
        return eventRepository.findByIdAndMatchIdAndDeletedAtIsNull(eventId, matchId)?.toStored()
    }

    @Transactional
    override fun update(eventId: UUID, event: Event, matchId: UUID): StoredEvent {
        val existing = eventRepository.findByIdAndMatchIdAndDeletedAtIsNull(eventId, matchId)
            ?: throw IllegalArgumentException("Event $eventId does not exist in match $matchId")
        require(event.type == existing.eventType) { "Event type is immutable" }
        require(event.occurredAt == existing.occurredAt) { "Event occurrence time is immutable" }
        val updated = EventEntity.fromDomain(existing.id, matchId, existing.sequenceNumber, event)
        eventRepository.save(updated)
        matchService.recalculateScore(matchId)
        return updated.toStored()
    }

    @Transactional
    override fun remove(eventId: UUID, matchId: UUID): Boolean {
        val existing = eventRepository.findByIdAndMatchIdAndDeletedAtIsNull(eventId, matchId) ?: return false
        eventRepository.save(existing.copy(deletedAt = Instant.now()))
        matchService.recalculateScore(matchId)
        return true
    }

    override fun getAllEventsOfMatch(matchId: UUID): List<StoredEvent> {
        matchService.getOrThrow(matchId)
        return eventRepository.findAllByMatchIdAndDeletedAtIsNullOrderBySequenceNumber(matchId).map { it.toStored() }
    }

    private fun EventEntity.toStored() = StoredEvent(id, sequenceNumber, toDomain())
}
