package com.github.mihanizzm.ultistats.model.events

import java.util.UUID

data class StoredEvent(
    val id: UUID,
    val sequenceNumber: Int,
    val event: Event,
)
