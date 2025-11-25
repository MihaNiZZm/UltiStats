package com.github.mihanizzm.ultistats.model.events

import java.util.UUID

sealed interface TeamEvent : Event {
    val team: UUID
}
