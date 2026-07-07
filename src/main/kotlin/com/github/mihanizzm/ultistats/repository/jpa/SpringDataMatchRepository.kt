package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.model.Match
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataMatchRepository : JpaRepository<Match, UUID>
