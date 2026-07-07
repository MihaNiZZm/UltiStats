package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.MatchFilterRequest
import com.github.mihanizzm.ultistats.exception.EntityNotFoundException
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.events.*
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataMatchRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
@Suppress("unused")
class MatchServiceImpl(
    private val matchRepository: SpringDataMatchRepository,
) : MatchService {
    private val log = LoggerFactory.getLogger(MatchServiceImpl::class.java)

    override fun get(matchId: UUID): Match? = matchRepository.findByIdOrNull(matchId)

    override fun getOrThrow(matchId: UUID): Match = matchRepository.findByIdOrNull(matchId)
        ?: throw EntityNotFoundException("Match $matchId not found")

    override fun create(match: Match) {
        matchRepository.save(match)
    }

    override fun update(match: Match) {
        matchRepository.save(match)
    }

    override fun delete(matchId: UUID) = matchRepository.deleteById(matchId)

    override fun getAll(): List<Match> = matchRepository.findAll()

    override fun findAllFiltered(filter: MatchFilterRequest): List<Match> =
        matchRepository.findAll().filter { it.matchesFilter(filter) }

    override fun count(): Long = matchRepository.count()

    override fun countFiltered(filter: MatchFilterRequest): Long =
        findAllFiltered(filter).size.toLong()

    override fun recalculateDiskHolder(matchId: UUID) {
        val match = getOrThrow(matchId)
        match.diskHolderId = calculateDiskHolder(match.events)
        update(match)
    }

    override fun startMatch(matchId: UUID, timestamp: Instant): Boolean {
        val match = getOrThrow(matchId)
        if (match.startedAt != null) {
            return false
        }
        match.startedAt = timestamp
        update(match)
        return true
    }

    override fun endMatch(matchId: UUID, timestamp: Instant): Boolean {
        val match = getOrThrow(matchId)
        if (match.startedAt == null || match.endedAt != null) {
            return false
        }
        match.endedAt = timestamp
        update(match)
        return true
    }

    private fun calculateDiskHolder(events: List<Event>): UUID? {
        var diskHolder: UUID? = null

        for (event in events) {
            diskHolder = when (event.type) {
                // Диск переходит к получателю
                EventType.PASS,
                EventType.INTERCEPTION -> (event as TwoPlayerEvent).toPlayer

                // Диск переходит к игроку, который подобрал
                EventType.TURNOVER -> (event as OnePlayerEvent).player

                // Диск никому не принадлежит (на земле или поинт завершён)
                EventType.DROP,
                EventType.BLOCK_MARKER,
                EventType.BLOCK_FIELD,
                EventType.GOAL,
                EventType.CALLAHAN -> null

                // Системные события не влияют на владельца диска
                EventType.PULL,
                EventType.BRICK,
                EventType.TIMEOUT_START,
                EventType.TIMEOUT_END,
                EventType.HALFTIME_START,
                EventType.HALFTIME_END -> diskHolder
            }
        }

        return diskHolder
    }

    private fun Match.matchesFilter(filter: MatchFilterRequest): Boolean {
        filter.teamId?.let { teamId ->
            if (!teamIds.contains(teamId)) {
                return false
            }
        }

        filter.status?.let { status ->
            if (this.status != status) {
                return false
            }
        }

        return true
    }
}
