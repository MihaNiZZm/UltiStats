package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.model.TeamPlayer
import com.github.mihanizzm.ultistats.model.TeamPlayerId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataTeamPlayerRepository : JpaRepository<TeamPlayer, TeamPlayerId> {
    fun findAllByTeamIdAndDeletedAtIsNull(teamId: UUID): List<TeamPlayer>
    fun findAllByPlayerIdAndDeletedAtIsNull(playerId: UUID): List<TeamPlayer>
    fun findByTeamIdAndPlayerIdAndDeletedAtIsNull(teamId: UUID, playerId: UUID): TeamPlayer?
}
