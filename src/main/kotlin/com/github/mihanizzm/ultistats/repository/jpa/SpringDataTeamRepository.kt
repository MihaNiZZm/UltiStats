package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.repository.entity.TeamEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SpringDataTeamRepository : JpaRepository<TeamEntity, UUID> {

    @Query("SELECT t FROM TeamEntity t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun findByNameContainingIgnoreCase(name: String): List<TeamEntity>

    @Query("SELECT COUNT(t) FROM TeamEntity t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun countByNameContainingIgnoreCase(name: String): Long

    fun findAllByIdIn(ids: List<UUID>): List<TeamEntity>
}
