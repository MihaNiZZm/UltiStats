package com.github.mihanizzm.ultistats.model.statistics

data class DefenseStatistics(
    /** Блоки, для которых не уточнена позиция защитника. */
    val blocks: Int = 0,
    val blocksAsMarker: Int = 0,
    val blocksAsFieldPlayer: Int = 0,
    val interceptions: Int = 0,
    val callahans: Int = 0,
)
