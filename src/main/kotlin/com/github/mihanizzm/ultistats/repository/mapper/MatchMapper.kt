package com.github.mihanizzm.ultistats.repository.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.mihanizzm.ultistats.model.Match
import com.github.mihanizzm.ultistats.model.TeamScore
import com.github.mihanizzm.ultistats.model.events.Event
import com.github.mihanizzm.ultistats.repository.entity.MatchEntity

class MatchMapper(private val objectMapper: ObjectMapper) {

    fun toDomain(entity: MatchEntity): Match {
        val events: MutableList<Event> = if (entity.eventsJson.isNotBlank() && entity.eventsJson != "[]") {
            objectMapper.readValue(entity.eventsJson)
        } else {
            mutableListOf()
        }

        val teamScores: MutableList<TeamScore> = if (entity.teamScoresJson.isNotBlank() && entity.teamScoresJson != "[]") {
            objectMapper.readValue(entity.teamScoresJson)
        } else {
            mutableListOf()
        }

        return Match(
            id = entity.id,
            teamIds = entity.teamIds.toList(),
            events = events,
            teamScores = teamScores,
            diskHolderId = entity.diskHolderId,
            plannedStartTimestamp = entity.plannedStartTimestamp,
            startedAt = entity.startedAt,
            endedAt = entity.endedAt,
        )
    }

    fun toEntity(match: Match): MatchEntity = MatchEntity(
        id = match.id,
        teamIds = match.teamIds.toTypedArray(),
        eventsJson = objectMapper.writeValueAsString(match.events),
        teamScoresJson = objectMapper.writeValueAsString(match.teamScores),
        diskHolderId = match.diskHolderId,
        plannedStartTimestamp = match.plannedStartTimestamp,
        startedAt = match.startedAt,
        endedAt = match.endedAt,
    )
}
