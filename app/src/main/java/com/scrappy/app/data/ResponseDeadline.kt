package com.scrappy.app.data

import java.net.SocketTimeoutException

/** Monotonic budget: receiving another byte must not restart the wait. */
class ResponseDeadline(timeoutMillis: Int, private val nanoTime: () -> Long = System::nanoTime) {
    private val budgetNanos = timeoutMillis.toLong() * 1_000_000L
    private val started = nanoTime()

    init { require(timeoutMillis > 0) }

    fun remainingMillis(): Int {
        val remaining = budgetNanos - (nanoTime() - started)
        if (remaining <= 0) throw SocketTimeoutException("AI analysis took too long. Retry or use the manual helper.")
        // Round up: a positive budget must never become an infinite socket timeout (0).
        return ((remaining + 999_999L) / 1_000_000L).toInt()
    }
}
