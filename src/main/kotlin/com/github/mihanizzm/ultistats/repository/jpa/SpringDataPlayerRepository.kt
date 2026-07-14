package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.model.Player
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SpringDataPlayerRepository : JpaRepository<Player, UUID> {

    fun findByIdAndDeletedAtIsNull(id: UUID): Player?

    fun findAllByIdInAndDeletedAtIsNull(ids: List<UUID>): List<Player>

    fun findAllByDeletedAtIsNull(): List<Player>

    @Query("""
        SELECT p FROM Player p
        WHERE p.deletedAt IS NULL
        AND LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))
    """)
    fun findFiltered(name: String): List<Player>

    @Query("""
        SELECT COUNT(p) FROM Player p
        WHERE p.deletedAt IS NULL
        AND LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))
    """)
    fun countFiltered(name: String): Long

    fun countByDeletedAtIsNull(): Long
}
