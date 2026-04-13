package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.dto.request.MatchFilterRequest
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.MatchStatus
import com.github.mihanizzm.ultistats.repository.entity.MatchEntity
import com.github.mihanizzm.ultistats.repository.mapper.MatchMapper
import com.github.mihanizzm.ultistats.service.MatchRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
@Profile("postgres")
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

    override fun findAllFiltered(filter: MatchFilterRequest): List<Match> {
        val spec = buildSpecification(filter)
        return springDataMatchRepository.findAll(spec).map { matchMapper.toDomain(it) }
    }

    override fun count(): Long = springDataMatchRepository.count()

    override fun countFiltered(filter: MatchFilterRequest): Long {
        val spec = buildSpecification(filter)
        return springDataMatchRepository.count(spec)
    }

    private fun buildSpecification(filter: MatchFilterRequest): Specification<MatchEntity> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            filter.teamId?.let { teamId ->
                // PostgreSQL array contains: team_ids @> ARRAY[teamId]
                val teamIdArray = criteriaBuilder.function(
                    "array_position",
                    Int::class.java,
                    root.get<Array<UUID>>("teamIds"),
                    criteriaBuilder.literal(teamId)
                )
                predicates.add(criteriaBuilder.isNotNull(teamIdArray))
            }

            filter.status?.let { status ->
                when (status) {
                    MatchStatus.FINISHED -> predicates.add(criteriaBuilder.isNotNull(root.get<Any>("endedAt")))
                    MatchStatus.IN_PROGRESS -> {
                        predicates.add(criteriaBuilder.isNotNull(root.get<Any>("startedAt")))
                        predicates.add(criteriaBuilder.isNull(root.get<Any>("endedAt")))
                    }
                    MatchStatus.PLANNED -> {
                        predicates.add(criteriaBuilder.isNull(root.get<Any>("startedAt")))
                        predicates.add(criteriaBuilder.isNull(root.get<Any>("endedAt")))
                    }
                }
            }

            filter.dateFrom?.let { dateFrom ->
                val dateField = criteriaBuilder.coalesce<java.time.Instant>(
                    root.get("startedAt"),
                    root.get("plannedStartTimestamp")
                )
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(dateField, dateFrom))
            }

            filter.dateTo?.let { dateTo ->
                val dateField = criteriaBuilder.coalesce<java.time.Instant>(
                    root.get("startedAt"),
                    root.get("plannedStartTimestamp")
                )
                predicates.add(criteriaBuilder.lessThanOrEqualTo(dateField, dateTo))
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}
