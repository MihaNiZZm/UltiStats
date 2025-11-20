package com.github.mihanizzm.ultistats.model.events

import java.util.UUID

sealed interface OnePlayerEvent : Event {
    val player: UUID
}