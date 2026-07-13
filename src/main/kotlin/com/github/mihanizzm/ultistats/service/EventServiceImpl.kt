package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.EventEntity
import com.github.mihanizzm.ultistats.model.events.Event
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
    override fun create(event: Event, matchId: UUID): UUID {
        matchService.getOrThrow(matchId)
        val sequenceNumber = (eventRepository.findFirstByMatchIdOrderBySequenceNumberDesc(matchId)
            ?.sequenceNumber ?: 0) + 1
        val entity = EventEntity.fromDomain(UUID.randomUUID(), matchId, sequenceNumber, event)
        eventRepository.save(entity)
        matchService.recalculateScore(matchId)
        return entity.id
    }

    @Transactional
    override fun edit(index: Int, event: Event, matchId: UUID): UUID {
        val existing = getEntities(matchId).getOrNull(index)
            ?: throw IllegalArgumentException("Event index $index does not exist")
        eventRepository.save(EventEntity.fromDomain(existing.id, matchId, existing.sequenceNumber, event))
        matchService.recalculateScore(matchId)
        return existing.id
    }

    @Transactional
    override fun remove(index: Int, matchId: UUID): UUID {
        val existing = getEntities(matchId).getOrNull(index)
            ?: throw IllegalArgumentException("Event index $index does not exist")
        eventRepository.save(existing.copy(deletedAt = Instant.now()))
        matchService.recalculateScore(matchId)
        return existing.id
    }

    override fun getAllEventsOfMatch(matchId: UUID): List<Event> {
        matchService.getOrThrow(matchId)
        return getEntities(matchId).map { it.toDomain() }
    }

    private fun getEntities(matchId: UUID): List<EventEntity> =
        eventRepository.findAllByMatchIdAndDeletedAtIsNullOrderBySequenceNumber(matchId)
}
