package com.github.mihanizzm.ultistats.repository.jpa

import com.github.mihanizzm.ultistats.repository.entity.MatchEntity
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

@Profile("postgres")
interface SpringDataMatchRepository : JpaRepository<MatchEntity, UUID>, JpaSpecificationExecutor<MatchEntity>
