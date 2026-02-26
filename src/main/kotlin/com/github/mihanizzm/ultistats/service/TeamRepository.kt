package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.TeamFilterRequest
import com.github.mihanizzm.ultistats.model.Team
import java.util.*

interface TeamRepository {
    fun get(id: UUID): Team?

    fun save(team: Team)

    fun delete(id: UUID)

    fun getAll(): List<Team>

    fun getAllInList(ids: List<UUID>): List<Team>

    fun findAllFiltered(filter: TeamFilterRequest): List<Team>

    fun count(): Long

    fun countFiltered(filter: TeamFilterRequest): Long
}