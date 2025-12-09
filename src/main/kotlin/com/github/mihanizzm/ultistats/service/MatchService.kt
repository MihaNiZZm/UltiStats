package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.Match
import java.util.UUID

interface MatchService {
    fun get(matchId: UUID): Match?

    fun getOrThrow(matchId: UUID): Match

    fun create(match: Match)

    fun delete(matchId: UUID)

    fun getAll(): List<Match>
}