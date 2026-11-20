package com.scrappy.app.data

import java.util.concurrent.CancellationException

/** Explicitly closes blocking network work; thread interruption alone cannot do that. */
class RequestCancellation {
    private val lock = Any()
    private var cancelled = false
    private var close: (() -> Unit)? = null

    fun check() {
        synchronized(lock) { if (cancelled) throw CancellationException("AI analysis cancelled") }
    }

    fun attach(action: () -> Unit) {
        val accepted = synchronized(lock) {
            check(close == null) { "A request is already attached" }
            if (cancelled) false else { close = action; true }
        }
        if (!accepted) {
            disconnectQuietly(action)
            throw CancellationException("AI analysis cancelled")
        }
    }

    fun detach() { synchronized(lock) { close = null } }

    fun cancel() {
        val action = synchronized(lock) {
            if (cancelled) return
            cancelled = true
            close.also { close = null }
        }
        action?.let(::disconnectQuietly)
    }

    // Cancellation is already recorded. A transport cleanup failure must not
    // replace CancellationException or escape into the camera lifecycle.
    private fun disconnectQuietly(action: () -> Unit) {
        try { action() } catch (_: Exception) { }
    }
}
