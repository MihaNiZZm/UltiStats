package com.github.mihanizzm.ultistats.service

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
}