package com.github.mihanizzm.ultistats.model.events

import java.time.Instant

sealed interface Event {
    val realTimestamp: Instant
    val type: EventType
}
