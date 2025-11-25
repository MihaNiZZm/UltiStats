package com.github.mihanizzm.ultistats.model.events

sealed interface Event {
    val time: Double // виртуальное время матча в секундах
    val type: EventType
}
