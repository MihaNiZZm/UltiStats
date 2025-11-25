package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.exception.EntityNotFoundException
import com.github.mihanizzm.ultistats.model.Team
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.logging.Logger

@Service
@Suppress("unused")
class TeamServiceImpl(
    private val teamRepository: TeamRepository,
) : TeamService {
    private val log = LoggerFactory.getLogger(TeamServiceImpl::class.java)

    override fun get(teamId: UUID): Team {
        val team = teamRepository.get(teamId)

        if (team == null) {
            val message = "Couldn't find match with id $teamId"
            log.info(message)
            throw EntityNotFoundException(message)
        }

        return team
    }

    override fun create(team: Team) = teamRepository.save(team)

    override fun delete(teamId: UUID) = teamRepository.delete(teamId)

    override fun getAll(): List<Team> = teamRepository.getAll()

    override fun getAllInList(ids: List<UUID>): List<Team> = teamRepository.getAllInList(ids)
}