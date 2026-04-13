package com.github.mihanizzm.ultistats.repository.jpa

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.repository.mapper.MatchMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("postgres")
class JpaRepositoryConfig {

    @Bean
    fun matchMapper(objectMapper: ObjectMapper): MatchMapper = MatchMapper(objectMapper)
}
