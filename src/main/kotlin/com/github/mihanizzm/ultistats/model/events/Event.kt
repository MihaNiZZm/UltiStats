package com.github.mihanizzm.ultistats.model.events

import java.util.UUID

sealed interface Event {
    val teamId: UUID
    val time: Double // виртуальное время матча в секундах
    val type: EventType
}

enum class EventType(
    val readableName: String,
) {
    PASS("Пас"),
    PULL("Пулл"),
    TURNOVER("Переход владения"),
    BRICK("Брик"),
    BLOCK_MARKER("Блок на маркере"),
    BLOCK_FIELD("Перебитие"),
    INTERCEPTION("Перехват"),
    CALLAHAN("Кэллахан"),
    TIMEOUT_START("Начало таймаута"),
    TIMEOUT_END("Конец таймаута"),
}