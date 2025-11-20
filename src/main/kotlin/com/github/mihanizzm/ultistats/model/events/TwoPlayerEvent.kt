package com.github.mihanizzm.ultistats.model.events

import java.util.UUID

sealed interface TwoPlayerEvent : Event {
    val fromPlayer: UUID
    val toPlayer: UUID
}