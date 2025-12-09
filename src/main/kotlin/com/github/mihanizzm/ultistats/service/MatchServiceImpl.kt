package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.exception.EntityNotFoundException
import com.github.mihanizzm.ultistats.model.Match
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("unused")
class MatchServiceImpl(
    private val matchRepository: MatchRepository,
) : MatchService {
    private val log = LoggerFactory.getLogger(MatchServiceImpl::class.java)

    override fun get(matchId: UUID): Match? = matchRepository.get(matchId)

    override fun getOrThrow(matchId: UUID): Match = matchRepository.get(matchId)
        ?: throw EntityNotFoundException("Match $matchId not found")

    override fun create(match: Match) = matchRepository.save(match)

    override fun delete(matchId: UUID) = matchRepository.delete(matchId)

    override fun getAll(): List<Match> = matchRepository.getAll()
}