package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.Team
import java.util.UUID

interface TeamService {
    fun get(teamId: UUID): Team?

    fun create(team: Team)

    fun delete(teamId: UUID)

    fun getAll(): List<Team>

    fun getAllInList(ids: List<UUID>): List<Team>
}