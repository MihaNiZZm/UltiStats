package com.github.mihanizzm.ultistats.validation.event

import com.github.mihanizzm.ultistats.model.events.EventType

sealed interface EventSequenceDecision {
    data class Allowed(val state: EventSequenceState) : EventSequenceDecision
    data class Rejected(val violation: EventSequenceViolation) : EventSequenceDecision
}

data class EventSequenceViolation(
    val eventIndex: Int,
    val currentState: String,
    val attemptedType: EventType?,
)
