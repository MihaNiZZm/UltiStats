package com.github.mihanizzm.ultistats

fun interface Verifiable : AutoCloseable {
    fun verify()

    override fun close() {
        verify()
    }

    fun compose(v: Verifiable): Verifiable = Verifiable {
        verify()
        v.verify()
    }
}