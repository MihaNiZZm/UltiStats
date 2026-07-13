package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.MatchFilterRequest
import com.github.mihanizzm.ultistats.exception.EntityNotFoundException
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.MatchPlayer
import com.github.mihanizzm.ultistats.model.MatchTeam
import com.github.mihanizzm.ultistats.model.TeamScore
import com.github.mihanizzm.ultistats.model.events.EventType
import com.github.mihanizzm.ultistats.model.events.TwoPlayerEvent
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataEventRepository
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataMatchPlayerRepository
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataMatchRepository
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataMatchTeamRepository
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataTeamPlayerRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class MatchServiceImpl(
    private val matchRepository: SpringDataMatchRepository,
    private val matchTeamRepository: SpringDataMatchTeamRepository,
    private val matchPlayerRepository: SpringDataMatchPlayerRepository,
    private val teamPlayerRepository: SpringDataTeamPlayerRepository,
    private val eventRepository: SpringDataEventRepository,
) : MatchService {
    override fun get(matchId: UUID): Match? =
        matchRepository.findByIdAndDeletedAtIsNull(matchId)?.hydrate(includeEvents = true)

    override fun getOrThrow(matchId: UUID): Match = get(matchId)
        ?: throw EntityNotFoundException("Match $matchId not found")

    @Transactional
    override fun create(match: Match) {
        matchRepository.save(match)
        replaceParticipants(match.id, match.teamIds)
    }

    @Transactional
    override fun update(match: Match) {
        val storedTeamIds = matchTeamRepository.findAllByMatchIdOrderByPosition(match.id).map { it.teamId }
        matchRepository.save(match)
        if (storedTeamIds != match.teamIds) replaceParticipants(match.id, match.teamIds)
    }

    override fun delete(matchId: UUID) {
        get(matchId)?.let {
            val deletedAt = Instant.now()
            eventRepository.findAllByMatchIdAndDeletedAtIsNull(matchId).forEach { event ->
                eventRepository.save(event.copy(deletedAt = deletedAt))
            }
            matchRepository.save(it.copy(deletedAt = deletedAt))
        }
    }

    override fun getAll(): List<Match> =
        matchRepository.findAllByDeletedAtIsNull().map { it.hydrate(includeEvents = false) }

    override fun findAllFiltered(filter: MatchFilterRequest): List<Match> =
        getAll().filter { match ->
            (filter.teamId == null || filter.teamId in match.teamIds) &&
                (filter.status == null || filter.status == match.status)
        }

    override fun count(): Long = matchRepository.countByDeletedAtIsNull()

    override fun countFiltered(filter: MatchFilterRequest): Long = findAllFiltered(filter).size.toLong()

    @Transactional
    override fun recalculateScore(matchId: UUID) {
        val match = getOrThrow(matchId)
        val scores = match.teamIds.associateWith { 0 }.toMutableMap()
        match.events.forEach { event ->
            if (event.type == EventType.GOAL || event.type == EventType.CALLAHAN) {
                val teamId = (event as TwoPlayerEvent).toTeam
                scores.computeIfPresent(teamId) { _, score -> score + 1 }
            }
        }
        matchTeamRepository.findAllByMatchIdOrderByPosition(matchId).forEach {
            it.score = scores.getValue(it.teamId)
            matchTeamRepository.save(it)
        }
    }

    override fun startMatch(matchId: UUID, timestamp: Instant): Boolean {
        val match = getOrThrow(matchId)
        if (match.startedAt != null) return false
        match.startedAt = timestamp
        matchRepository.save(match)
        return true
    }

    override fun endMatch(matchId: UUID, timestamp: Instant): Boolean {
        val match = getOrThrow(matchId)
        if (match.startedAt == null || match.endedAt != null) return false
        match.endedAt = timestamp
        matchRepository.save(match)
        return true
    }

    private fun Match.hydrate(includeEvents: Boolean): Match {
        val matchTeams = matchTeamRepository.findAllByMatchIdOrderByPosition(id)
        val matchPlayers = matchPlayerRepository.findAllByMatchId(id)
        val teamByPlayerId = matchPlayers.associate { it.playerId to it.teamId }
        val events = if (includeEvents) {
            eventRepository.findAllByMatchIdAndDeletedAtIsNullOrderBySequenceNumber(id)
                .map { it.toDomain(teamByPlayerId) }
                .toMutableList()
        } else {
            mutableListOf()
        }
        return copy(
            teamIds = matchTeams.map { it.teamId },
            teamScores = matchTeams.map { TeamScore(it.teamId, it.score) }.toMutableList(),
            playerIdsByTeam = matchPlayers.groupBy({ it.teamId }, { it.playerId }),
            events = events,
            eventCount = if (includeEvents) events.size else
                eventRepository.countByMatchIdAndDeletedAtIsNull(id).toInt(),
        )
    }

    private fun replaceParticipants(matchId: UUID, teamIds: List<UUID>) {
        val existingTeams = matchTeamRepository.findAllByMatchIdOrderByPosition(matchId)
        if (existingTeams.map { it.teamId } == teamIds) return
        matchPlayerRepository.deleteAllByMatchId(matchId)
        matchPlayerRepository.flush()
        matchTeamRepository.deleteAllByMatchId(matchId)
        matchTeamRepository.flush()
        matchTeamRepository.saveAll(teamIds.mapIndexed { index, teamId ->
            MatchTeam(matchId, teamId, index + 1)
        })
        val players = teamIds.flatMap { teamId ->
            teamPlayerRepository.findAllByTeamIdAndDeletedAtIsNull(teamId).map { membership ->
                MatchPlayer(matchId, membership.playerId, teamId, membership.number)
            }
        }
        matchPlayerRepository.saveAll(players)
    }
}
