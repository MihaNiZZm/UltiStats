package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.model.MatchTeam
import com.github.mihanizzm.ultistats.model.MatchTeamId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataMatchTeamRepository : JpaRepository<MatchTeam, MatchTeamId> {
    fun findAllByMatchIdOrderByPosition(matchId: UUID): List<MatchTeam>
    fun findAllByTeamId(teamId: UUID): List<MatchTeam>
    fun deleteAllByMatchId(matchId: UUID)
}
