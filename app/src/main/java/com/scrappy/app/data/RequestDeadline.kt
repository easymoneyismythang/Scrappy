package com.scrappy.app.data

import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Covers blocking upload as well as response work; socket readTimeout cannot bound writes. */
internal class RequestDeadline(timeoutMillis: Int, disconnect: () -> Unit) : AutoCloseable {
    private val budget = ResponseDeadline(timeoutMillis)
    private val lock = Any()
    private var closed = false
    private var expired = false
    // Each active request owns its watchdog so one slow disconnect cannot delay another.
    private val watchdog = Executors.newSingleThreadScheduledExecutor { work ->
        Thread(work, "scrappy-photo-deadline").apply { isDaemon = true }
    }
    private val task = watchdog.schedule({
        val shouldDisconnect = synchronized(lock) {
            if (closed) false else { expired = true; true }
        }
        if (shouldDisconnect) runCatching { disconnect() }
    }, timeoutMillis.toLong(), TimeUnit.MILLISECONDS)

    fun check() {
        if (synchronized(lock) { expired }) throw timeout()
        budget.remainingMillis()
    }

    override fun close() {
        synchronized(lock) { closed = true }
        task.cancel(false)
        watchdog.shutdown()
    }

    private fun timeout() = SocketTimeoutException("AI analysis took too long. Retry or use the manual helper.")
}
