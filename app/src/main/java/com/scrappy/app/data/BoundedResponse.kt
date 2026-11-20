package com.scrappy.app.data

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Read at most limit bytes, plus one byte to detect an oversized response. */
object BoundedResponse {
    fun read(input: InputStream, limit: Int, checkpoint: () -> Unit = {}): ByteArray {
        require(limit in 1 until Int.MAX_VALUE)
        val output = ByteArrayOutputStream(minOf(limit, 4096))
        val buffer = ByteArray(minOf(limit + 1, 4096))
        while (true) {
            checkpoint()
            val count = input.read(buffer, 0, minOf(buffer.size, limit - output.size() + 1))
            checkpoint()
            if (count < 0) return output.toByteArray()
            if (count == 0) {
                checkpoint()
                val byte = input.read()
                checkpoint()
                if (byte < 0) return output.toByteArray()
                check(output.size() < limit) { "Service response was too large" }
                output.write(byte)
            } else {
                check(count <= limit - output.size()) { "Service response was too large" }
                output.write(buffer, 0, count)
            }
        }
    }
}
