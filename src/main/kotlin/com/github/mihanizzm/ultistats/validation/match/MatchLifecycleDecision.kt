package com.github.mihanizzm.ultistats.validation.match

sealed interface MatchLifecycleDecision {
    data object Allowed : MatchLifecycleDecision
    data class InvalidState(val problem: MatchProblem) : MatchLifecycleDecision
    data class Conflict(val problem: MatchProblem) : MatchLifecycleDecision
}
