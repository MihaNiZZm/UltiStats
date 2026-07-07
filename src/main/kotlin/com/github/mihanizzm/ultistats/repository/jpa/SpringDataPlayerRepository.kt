package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.repository.entity.PlayerEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SpringDataPlayerRepository : JpaRepository<PlayerEntity, UUID> {

    fun findAllByTeamId(teamId: UUID): List<PlayerEntity>

    fun findAllByIdIn(ids: List<UUID>): List<PlayerEntity>

    @Query("""
        SELECT p FROM PlayerEntity p
        WHERE (:teamId IS NULL OR p.teamId = :teamId)
        AND (:name IS NULL OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))
    """)
    fun findFiltered(teamId: UUID?, name: String?): List<PlayerEntity>

    @Query("""
        SELECT COUNT(p) FROM PlayerEntity p
        WHERE (:teamId IS NULL OR p.teamId = :teamId)
        AND (:name IS NULL OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))
    """)
    fun countFiltered(teamId: UUID?, name: String?): Long
}
