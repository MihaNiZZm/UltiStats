package com.github.mihanizzm.ultistats.model.events

/**
 * Категория события, определяющая набор обязательных полей.
 */
enum class EventCategory {
    /**
     * Событие с одним игроком. Требует: player, team.
     */
    ONE_PLAYER,

    /**
     * Событие с двумя игроками. Требует: fromPlayer, toPlayer, fromTeam, toTeam.
     */
    TWO_PLAYER,

    /**
     * Событие команды. Требует: team.
     */
    TEAM,

    /**
     * Системное событие. Не требует дополнительных полей.
     */
    SYSTEM,
}
