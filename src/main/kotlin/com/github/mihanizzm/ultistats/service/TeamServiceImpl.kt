package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.TeamFilterRequest
import com.github.mihanizzm.ultistats.model.Team
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataPlayerRepository
import com.github.mihanizzm.ultistats.repository.jpa.SpringDataTeamRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("unused")
class TeamServiceImpl(
    private val teamRepository: SpringDataTeamRepository,
    private val playerRepository: SpringDataPlayerRepository,
) : TeamService {
    private val log = LoggerFactory.getLogger(TeamServiceImpl::class.java)

    override fun get(teamId: UUID): Team? = teamRepository.findByIdOrNull(teamId)?.withPlayerIds()

    override fun create(team: Team) {
        teamRepository.save(team)
    }

    override fun update(team: Team) {
        teamRepository.save(team)
    }

    override fun delete(teamId: UUID) = teamRepository.deleteById(teamId)

    override fun getAll(): List<Team> = teamRepository.findAll().withPlayerIds()

    override fun getAllInList(ids: List<UUID>): List<Team> = teamRepository.findAllByIdIn(ids).withPlayerIds()

    override fun findAllFiltered(filter: TeamFilterRequest): List<Team> {
        val teams = if (filter.name != null) {
            teamRepository.findByNameContainingIgnoreCase(filter.name)
        } else {
            teamRepository.findAll()
        }
        return teams.withPlayerIds()
    }

    override fun count(): Long = teamRepository.count()

    override fun countFiltered(filter: TeamFilterRequest): Long =
        if (filter.name != null) {
            teamRepository.countByNameContainingIgnoreCase(filter.name)
        } else {
            teamRepository.count()
        }

    private fun List<Team>.withPlayerIds(): List<Team> = map { it.withPlayerIds() }

    private fun Team.withPlayerIds(): Team =
        copy(playerIds = playerRepository.findAllByTeamId(id).map { it.id })
}
