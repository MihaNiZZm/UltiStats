package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.dto.request.MatchFilterRequest
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.repository.mapper.MatchMapper
import com.github.mihanizzm.ultistats.service.MatchRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaMatchRepositoryAdapter(
    private val springDataMatchRepository: SpringDataMatchRepository,
    private val matchMapper: MatchMapper,
) : MatchRepository {

    override fun get(id: UUID): Match? =
        springDataMatchRepository.findByIdOrNull(id)?.let { matchMapper.toDomain(it) }

    override fun save(match: Match) {
        springDataMatchRepository.save(matchMapper.toEntity(match))
    }

    override fun delete(id: UUID) {
        springDataMatchRepository.deleteById(id)
    }

    override fun getAll(): List<Match> =
        springDataMatchRepository.findAll().map { matchMapper.toDomain(it) }

    override fun findAllFiltered(filter: MatchFilterRequest): List<Match> =
        getAll().filter { match -> matchesFilter(match, filter) }

    override fun count(): Long = springDataMatchRepository.count()

    override fun countFiltered(filter: MatchFilterRequest): Long =
        findAllFiltered(filter).size.toLong()

    private fun matchesFilter(match: Match, filter: MatchFilterRequest): Boolean {
        if (filter.teamId != null && filter.teamId !in match.teamIds) {
            return false
        }
        if (filter.status != null && match.status != filter.status) {
            return false
        }
        if (filter.dateFrom != null || filter.dateTo != null) {
            val matchDate = match.startedAt ?: match.plannedStartTimestamp
            if (matchDate == null) {
                return false
            }
            if (filter.dateFrom != null && matchDate < filter.dateFrom) {
                return false
            }
            if (filter.dateTo != null && matchDate > filter.dateTo) {
                return false
            }
        }
        return true
    }
}
