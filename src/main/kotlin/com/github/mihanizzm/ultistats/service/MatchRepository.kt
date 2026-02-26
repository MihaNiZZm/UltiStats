package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.dto.request.MatchFilterRequest
import com.github.mihanizzm.ultistats.model.Match
import java.util.UUID

interface MatchRepository {
    fun get(id: UUID): Match?

    fun save(match: Match)

    fun delete(id: UUID)

    fun getAll(): List<Match>

    fun findAllFiltered(filter: MatchFilterRequest): List<Match>

    fun count(): Long

    fun countFiltered(filter: MatchFilterRequest): Long
}