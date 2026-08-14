package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.EventEntity
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.model.events.StoredEvent
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataEventRepository
import com.github.mihanizzm.ultistats.service.result.EventCommandResult
import com.github.mihanizzm.ultistats.validation.match.MatchLifecycleDecision
import com.github.mihanizzm.ultistats.validation.match.MatchLifecyclePolicy
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class EventServiceImpl(
    private val eventRepository: SpringDataEventRepository,
    private val matchService: MatchService,
    private val lifecyclePolicy: MatchLifecyclePolicy,
) : EventService {
    @Transactional
    override fun create(event: Event, matchId: UUID): EventCommandResult {
        val lockedMatch = matchService.getForUpdate(matchId) ?: return EventCommandResult.NotFound
        val activeEvents = eventRepository.findAllByMatchIdAndDeletedAtIsNullOrderBySequenceNumber(matchId)
        val match = lockedMatch.toPolicyMatch(activeEvents)
        lifecyclePolicy.validateEventCreation(match, event.occurredAt).toCommandRejection()?.let { return it }

        val lastInSequence = eventRepository.findFirstByMatchIdOrderBySequenceNumberDesc(matchId)
        val entity = EventEntity.fromDomain(UUID.randomUUID(), matchId, (lastInSequence?.sequenceNumber ?: 0) + 1, event)
        eventRepository.save(entity)
        matchService.recalculateScore(matchId)
        return EventCommandResult.Success(entity.toStored())
    }

    override fun get(eventId: UUID, matchId: UUID): StoredEvent? {
        matchService.getOrThrow(matchId)
        return eventRepository.findByIdAndMatchIdAndDeletedAtIsNull(eventId, matchId)?.toStored()
    }

    @Transactional
    override fun update(eventId: UUID, event: Event, matchId: UUID): EventCommandResult {
        val match = matchService.getForUpdate(matchId) ?: return EventCommandResult.NotFound
        lifecyclePolicy.validateEventUpdate(match).toCommandRejection()?.let { return it }

        val existing = eventRepository.findByIdAndMatchIdAndDeletedAtIsNull(eventId, matchId)
            ?: return EventCommandResult.NotFound
        require(event.type == existing.eventType) { "Event type is immutable" }
        require(event.occurredAt == existing.occurredAt) { "Event occurrence time is immutable" }
        val updated = EventEntity.fromDomain(existing.id, matchId, existing.sequenceNumber, event)
        eventRepository.save(updated)
        matchService.recalculateScore(matchId)
        return EventCommandResult.Success(updated.toStored())
    }

    @Transactional
    override fun remove(eventId: UUID, matchId: UUID): EventCommandResult {
        val match = matchService.getForUpdate(matchId) ?: return EventCommandResult.NotFound
        lifecyclePolicy.validateEventDeletion(match).toCommandRejection()?.let { return it }

        val existing = eventRepository.findByIdAndMatchIdAndDeletedAtIsNull(eventId, matchId)
            ?: return EventCommandResult.NotFound
        eventRepository.save(existing.copy(deletedAt = Instant.now()))
        matchService.recalculateScore(matchId)
        return EventCommandResult.Deleted
    }

    override fun getAllEventsOfMatch(matchId: UUID): List<StoredEvent> {
        matchService.getOrThrow(matchId)
        return eventRepository.findAllByMatchIdAndDeletedAtIsNullOrderBySequenceNumber(matchId).map { it.toStored() }
    }

    private fun EventEntity.toStored() = StoredEvent(id, sequenceNumber, toDomain())

    private fun Match.toPolicyMatch(activeEvents: List<EventEntity>) = Match(
        id = id,
        teamIds = emptyList(),
        events = activeEvents.map { it.toDomain() }.toMutableList(),
        plannedStartTimestamp = plannedStartTimestamp,
        startedAt = startedAt,
        endedAt = endedAt,
        deletedAt = deletedAt,
    )

    private fun MatchLifecycleDecision.toCommandRejection(): EventCommandResult? = when (this) {
        MatchLifecycleDecision.Allowed -> null
        is MatchLifecycleDecision.InvalidState -> EventCommandResult.InvalidState(problem)
        is MatchLifecycleDecision.Conflict -> EventCommandResult.Conflict(problem)
    }
}
