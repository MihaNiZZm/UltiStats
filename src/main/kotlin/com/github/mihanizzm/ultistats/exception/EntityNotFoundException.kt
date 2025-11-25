package com.github.mihanizzm.ultistats.exception

import java.lang.RuntimeException

class EntityNotFoundException(
    override val message: String
) : RuntimeException(message)