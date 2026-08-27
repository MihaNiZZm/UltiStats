package com.github.mihanizzm.ultistats.config

import com.github.mihanizzm.ultistats.validation.event.EventSequencePolicy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ValidationConfig {
    @Bean
    fun eventSequencePolicy(): EventSequencePolicy = EventSequencePolicy()
}
