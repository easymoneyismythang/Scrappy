package com.scrappy.app

import com.scrappy.app.data.BoundedResponse
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.*
import org.junit.Test

class BoundedResponseTest {
    @Test fun acceptsEmptyAndExactLimit() {
        assertArrayEquals(byteArrayOf(), BoundedResponse.read(ByteArrayInputStream(byteArrayOf()), 4))
        val bytes = byteArrayOf(1, 2, 3, 4)
        assertArrayEquals(bytes, BoundedResponse.read(ByteArrayInputStream(bytes), 4))
    }
    @Test fun stopsAfterOneExcessByte() {
        var consumed = 0
        val stream = object : InputStream() {
            override fun read(): Int { consumed++; return 42 }
        }
        try {
            BoundedResponse.read(stream, 4)
            fail("Oversized response accepted")
        } catch (_: IllegalStateException) {
            assertEquals(5, consumed)
        }
    }
    @Test fun handlesShortAndZeroLengthReads() {
        val source = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        var first = true
        val stream = object : InputStream() {
            override fun read() = source.read()
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (first) { first = false; return 0 }
                return source.read(b, off, minOf(len, 1))
            }
        }
        assertArrayEquals(byteArrayOf(1, 2, 3), BoundedResponse.read(stream, 4))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsInvalidLimit() {
        BoundedResponse.read(ByteArrayInputStream(byteArrayOf()), 0)
    }
}
