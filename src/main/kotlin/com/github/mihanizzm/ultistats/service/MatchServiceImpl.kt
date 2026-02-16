package com.github.mihanizzm.ultistats.service

import com.github.mihanizzm.ultistats.exception.EntityNotFoundException
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.events.*
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

    override fun recalculateDiskHolder(matchId: UUID) {
        val match = getOrThrow(matchId)
        match.diskHolderId = calculateDiskHolder(match.events)
    }

    private fun calculateDiskHolder(events: List<Event>): UUID? {
        var diskHolder: UUID? = null

        for (event in events) {
            diskHolder = when (event) {
                // Диск переходит к получателю
                is PassEvent -> event.toPlayer
                is InterceptionEvent -> event.toPlayer

                // Диск переходит к игроку, который подобрал
                is TurnoverEvent -> event.player

                // Диск никому не принадлежит (на земле или поинт завершён)
                is DropEvent,
                is BlockMarkerEvent,
                is BlockFieldEvent,
                is GoalEvent,
                is CallahanEvent -> null

                // Системные события не влияют на владельца диска
                is PullEvent,
                is BrickEvent,
                is TimeoutStartEvent,
                is TimeoutEndEvent,
                is HalftimeStartEvent,
                is HalftimeEndEvent -> diskHolder
            }
        }

        return diskHolder
    }
}