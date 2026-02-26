package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.MatchFilterRequest
import com.github.mihanizzm.ultistats.model.Match
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
@Suppress("unused")
class MatchRepositoryConcurrentMapImpl : MatchRepository {
    private val matches = ConcurrentHashMap<UUID, Match>()

    override fun get(id: UUID): Match? = matches[id]

    override fun save(match: Match) {
        matches[match.id] = match
    }

    override fun delete(id: UUID) {
        matches.remove(id)
    }

    override fun getAll(): List<Match> = matches.values.toList()

    override fun findAllFiltered(filter: MatchFilterRequest): List<Match> =
        matches.values.filter { match -> matchesFilter(match, filter) }.toList()

    override fun count(): Long = matches.size.toLong()

    override fun countFiltered(filter: MatchFilterRequest): Long =
        matches.values.count { match -> matchesFilter(match, filter) }.toLong()

    private fun matchesFilter(match: Match, filter: MatchFilterRequest): Boolean {
        if (filter.teamId != null) {
            val teamIds = match.teams.map { it.id }
            if (filter.teamId !in teamIds) {
                return false
            }
        }
        if (filter.status != null && match.status != filter.status) {
            return false
        }
        if (filter.dateFrom != null || filter.dateTo != null) {
            val matchDate = match.startedAt ?: match.plannedStartTimestamp
            if (matchDate != null) {
                if (filter.dateFrom != null && matchDate < filter.dateFrom) {
                    return false
                }
                if (filter.dateTo != null && matchDate > filter.dateTo) {
                    return false
                }
            } else if (filter.dateFrom != null || filter.dateTo != null) {
                return false
            }
        }
        return true
    }
}