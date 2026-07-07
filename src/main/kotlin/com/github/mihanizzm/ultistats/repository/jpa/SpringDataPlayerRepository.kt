package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.model.Player
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SpringDataPlayerRepository : JpaRepository<Player, UUID> {

    fun findAllByTeamId(teamId: UUID): List<Player>

    fun findAllByIdIn(ids: List<UUID>): List<Player>

    @Query("""
        SELECT p FROM Player p
        WHERE (:teamId IS NULL OR p.teamId = :teamId)
        AND (:name IS NULL OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))
    """)
    fun findFiltered(teamId: UUID?, name: String?): List<Player>

    @Query("""
        SELECT COUNT(p) FROM Player p
        WHERE (:teamId IS NULL OR p.teamId = :teamId)
        AND (:name IS NULL OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))
    """)
    fun countFiltered(teamId: UUID?, name: String?): Long
}
