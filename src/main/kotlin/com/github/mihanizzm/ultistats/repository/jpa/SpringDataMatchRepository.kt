package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.repository.entity.MatchEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

interface SpringDataMatchRepository : JpaRepository<MatchEntity, UUID>, JpaSpecificationExecutor<MatchEntity>
