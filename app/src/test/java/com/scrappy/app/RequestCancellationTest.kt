package com.scrappy.app

import com.scrappy.app.data.RequestCancellation
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class RequestCancellationTest {
    @Test fun throwingDisconnectDoesNotEscapeCancellationOrRunTwice() {
        val cancellation=RequestCancellation();var closes=0
        cancellation.attach { closes++;throw IllegalStateException("Cleanup failed") }
        cancellation.cancel();cancellation.cancel()
        assertEquals(1,closes)
        assertTrue(runCatching { cancellation.check() }.exceptionOrNull() is CancellationException)
    }

    @Test fun lateAttachStillReportsCancellationWhenDisconnectThrows() {
        val cancellation=RequestCancellation();cancellation.cancel()
        val failure=runCatching {
            cancellation.attach { throw IllegalStateException("Cleanup failed") }
        }.exceptionOrNull()
        assertTrue(failure is CancellationException)
    }

    @Test fun leavingClosesActiveRequestExactlyOnce() {
        val cancellation=RequestCancellation();var closes=0
        cancellation.attach { closes++ }
        cancellation.cancel();cancellation.cancel()
        assertEquals(1,closes)
        try { cancellation.check();fail("Cancelled work continued") } catch (_: CancellationException) { }
    }

    @Test fun leavingBeforeConnectionCreationPreventsUpload() {
        val cancellation=RequestCancellation();var closed=false
        cancellation.cancel()
        try { cancellation.attach { closed=true };fail("Late connection accepted") } catch (_: CancellationException) { }
        assertTrue(closed)
    }

    @Test fun completedConnectionIsNotClosedAgainByCancellation() {
        val cancellation=RequestCancellation();var closes=0
        cancellation.attach { closes++ };cancellation.detach();cancellation.cancel()
        assertEquals(0,closes)
    }

    @Test fun cancellationReleasesBlockedNetworkWorker() {
        val cancellation=RequestCancellation()
        val ready=CountDownLatch(1);val closed=CountDownLatch(1);val finished=CountDownLatch(1)
        val worker=Thread {
            cancellation.attach { closed.countDown() };ready.countDown()
            if (closed.await(2,TimeUnit.SECONDS)) finished.countDown()
        }
        worker.start()
        assertTrue(ready.await(2,TimeUnit.SECONDS))
        cancellation.cancel()
        assertTrue(finished.await(2,TimeUnit.SECONDS));worker.join(2000)
        assertFalse(worker.isAlive)
    }
}
