package com.scrappy.app

import com.scrappy.app.data.RequestDeadline
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class RequestDeadlineTest {
    @Test fun completionCancelsPendingDisconnect() {
        val disconnected = CountDownLatch(1)
        val deadline = RequestDeadline(100) { disconnected.countDown() }
        deadline.check(); deadline.close(); deadline.close()
        assertFalse(disconnected.await(250, TimeUnit.MILLISECONDS))
    }

    @Test fun expiryDisconnectsOnceAndRejectsResultEvenIfDisconnectFails() {
        val disconnected = CountDownLatch(1)
        val calls = AtomicInteger()
        val deadline = RequestDeadline(50) {
            calls.incrementAndGet(); disconnected.countDown(); error("Adapter failure")
        }
        try {
            assertTrue(disconnected.await(2, TimeUnit.SECONDS))
            assertTrue(runCatching { deadline.check() }.exceptionOrNull() is SocketTimeoutException)
            assertEquals(1, calls.get())
        } finally { deadline.close() }
    }

    @Test fun invalidBudgetRejectedBeforeScheduling() {
        for (budget in listOf(0, -1)) {
            assertTrue(runCatching { RequestDeadline(budget) { fail("Unexpected disconnect") } }
                .exceptionOrNull() is IllegalArgumentException)
        }
    }
}
