package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.model.MatchPlayer
import com.github.mihanizzm.ultistats.model.MatchPlayerId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataMatchPlayerRepository : JpaRepository<MatchPlayer, MatchPlayerId> {
    fun findAllByMatchId(matchId: UUID): List<MatchPlayer>
    fun deleteAllByMatchId(matchId: UUID)
}
