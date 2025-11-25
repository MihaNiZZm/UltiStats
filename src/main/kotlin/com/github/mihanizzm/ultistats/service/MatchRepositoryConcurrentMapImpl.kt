package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.model.Match
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
@Suppress("unused")
class MatchRepositoryConcurrentMapImpl : MatchRepository {
    private val matches = ConcurrentHashMap<UUID, Match>()

    override fun get(id: UUID): Match? = matches[id]

    override fun save(match: Match) {
        matches[match.id] = match
    }

    override fun delete(id: UUID) {
        matches.remove(id)
    }

    override fun getAll(): List<Match> = matches.values.toList()
}