package com.github.mihanizzm.ultistats.factory

import com.github.mihanizzm.ultistats.dto.request.CreateEventRequest
import com.github.mihanizzm.ultistats.model.events.*
import org.springframework.stereotype.Component

@Component
class EventFactory {
    fun createFromRequest(request: CreateEventRequest): Event? {
        return when (request.type) {
            EventType.PASS -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                PassEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.GOAL -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                GoalEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.DROP -> {
                if (request.playerId == null) return null
                DropEvent(
                    player = request.playerId,
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.PULL -> {
                if (request.playerId == null) return null
                PullEvent(
                    player = request.playerId,
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.BRICK -> {
                if (request.playerId == null) return null
                BrickEvent(
                    player = request.playerId,
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.TURNOVER -> {
                if (request.playerId == null) return null
                TurnoverEvent(
                    player = request.playerId,
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.BLOCK_MARKER -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                BlockMarkerEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.BLOCK_FIELD -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                BlockFieldEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.INTERCEPTION -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                InterceptionEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.CALLAHAN -> {
                if (request.playerId == null || request.toPlayerId == null || request.toTeamId == null) return null
                CallahanEvent(
                    fromPlayer = request.playerId,
                    toPlayer = request.toPlayerId,
                    fromTeam = request.teamId,
                    toTeam = request.toTeamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.TIMEOUT_START -> {
                TimeoutStartEvent(
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.TIMEOUT_END -> {
                TimeoutEndEvent(
                    team = request.teamId,
                    realTimestamp = request.timestamp,
                )
            }
            EventType.HALFTIME_START -> {
                HalftimeStartEvent(
                    realTimestamp = request.timestamp,
                )
            }
            EventType.HALFTIME_END -> {
                HalftimeEndEvent(
                    realTimestamp = request.timestamp,
                )
            }
        }
    }
}
