package com.github.mihanizzm.ultistats.controller

import com.github.mihanizzm.ultistats.validation.match.MatchProblem
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.net.URI

fun MatchProblem.toProblemDetail(status: HttpStatus, instance: URI): ProblemDetail =
    ProblemDetail.forStatusAndDetail(status, detail).apply {
        title = this@toProblemDetail.title
        this.instance = instance
        setProperty("code", code.name)
        currentStatus?.let { setProperty("currentStatus", it.name) }
        currentState?.let { setProperty("currentState", it) }
        attemptedEventType?.let { setProperty("attemptedEventType", it.name) }
    }
