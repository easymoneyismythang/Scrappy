package com.scrappy.app

import com.scrappy.app.data.BoundedResponse
import com.scrappy.app.data.ResponseDeadline
import java.io.InputStream
import java.net.SocketTimeoutException
import org.junit.Assert.*
import org.junit.Test

class ResponseDeadlineTest {
    @Test fun roundsPositiveBudgetUpAndExpiresAtDeadline() {
        var now=0L
        val deadline=ResponseDeadline(45_000) { now }
        assertEquals(45_000,deadline.remainingMillis())
        now=44_999_999_999L
        assertEquals(1,deadline.remainingMillis())
        now=45_000_000_000L
        try { deadline.remainingMillis();fail("Expired deadline accepted") }
        catch (_: SocketTimeoutException) { }
    }

    @Test fun slowTrickleCannotExtendTheBudget() {
        var now=0L
        var reads=0
        val deadline=ResponseDeadline(100) { now }
        val input=object : InputStream() {
            override fun read(): Int { reads++;now+=30_000_000L;return 42 }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                b[off]=read().toByte();return 1
            }
        }
        try { BoundedResponse.read(input,100) { deadline.remainingMillis() };fail("Trickle never expired") }
        catch (_: SocketTimeoutException) { assertEquals(4,reads) }
    }

    @Test fun checksDeadlineAfterZeroReadFallbackAndEof() {
        for (lastByte in listOf(-1,42)) {
            var now=0L
            val deadline=ResponseDeadline(100) { now }
            val input=object : InputStream() {
                override fun read(): Int { now=100_000_000L;return lastByte }
                override fun read(b: ByteArray, off: Int, len: Int)=0
            }
            try { BoundedResponse.read(input,4) { deadline.remainingMillis() };fail("Late result accepted") }
            catch (_: SocketTimeoutException) { }
        }
    }

    @Test fun acceptsTimelyResponseIncludingShortReads() {
        var now=0L
        var index=0
        val deadline=ResponseDeadline(100) { now }
        val input=object : InputStream() {
            override fun read(): Int { now+=10_000_000L;return if(index++<3) index else -1 }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val value=read();if(value<0)return -1;b[off]=value.toByte();return 1
            }
        }
        assertArrayEquals(byteArrayOf(1,2,3),BoundedResponse.read(input,4) { deadline.remainingMillis() })
    }
}
