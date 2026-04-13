package com.github.mihanizzm.ultistats.repository.inmemory

import com.github.mihanizzm.ultistats.dto.request.TeamFilterRequest
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.service.TeamRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Repository
@Profile("inmemory", "default")
class InMemoryTeamRepository : TeamRepository {
    private val teams = ConcurrentHashMap<UUID, Team>()

    override fun get(id: UUID): Team? = teams[id]

    override fun save(team: Team) {
        teams[team.id] = team
    }

    override fun delete(id: UUID) {
        teams.remove(id)
    }

    override fun getAll(): List<Team> = teams.values.toList()

    override fun getAllInList(ids: List<UUID>): List<Team> = ids.mapNotNull { get(it) }

    override fun findAllFiltered(filter: TeamFilterRequest): List<Team> =
        teams.values.filter { team -> matchesFilter(team, filter) }.toList()

    override fun count(): Long = teams.size.toLong()

    override fun countFiltered(filter: TeamFilterRequest): Long =
        teams.values.count { team -> matchesFilter(team, filter) }.toLong()

    private fun matchesFilter(team: Team, filter: TeamFilterRequest): Boolean {
        if (filter.name != null) {
            val searchName = filter.name.lowercase()
            if (!team.name.lowercase().contains(searchName)) {
                return false
            }
        }
        return true
    }
}
