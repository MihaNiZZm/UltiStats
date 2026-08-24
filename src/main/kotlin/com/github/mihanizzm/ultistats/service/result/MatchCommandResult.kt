package com.github.mihanizzm.ultistats.service.result

import com.github.mihanizzm.ultistats.validation.match.MatchProblem

sealed interface MatchCommandResult<out T> {
    data class Success<T>(val value: T) : MatchCommandResult<T>
    data object NotFound : MatchCommandResult<Nothing>
    data class InvalidRequest(val problem: MatchProblem) : MatchCommandResult<Nothing>
    data class InvalidState(val problem: MatchProblem) : MatchCommandResult<Nothing>
    data class Conflict(val problem: MatchProblem) : MatchCommandResult<Nothing>
}
