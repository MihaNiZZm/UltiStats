package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.model.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SpringDataTeamRepository : JpaRepository<Team, UUID> {

    @Query("SELECT t FROM Team t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun findByNameContainingIgnoreCase(name: String): List<Team>

    @Query("SELECT COUNT(t) FROM Team t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun countByNameContainingIgnoreCase(name: String): Long

    fun findAllByIdIn(ids: List<UUID>): List<Team>
}
