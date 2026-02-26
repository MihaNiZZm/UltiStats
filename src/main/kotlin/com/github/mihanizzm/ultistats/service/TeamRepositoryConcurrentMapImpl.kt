package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.TeamFilterRequest
import com.github.mihanizzm.ultistats.model.Team
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
@Suppress("unused")
class TeamRepositoryConcurrentMapImpl : TeamRepository {
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