package com.scrappy.app

import com.scrappy.app.data.AiHttpClient
import com.scrappy.app.data.RequestCancellation
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class AiHttpClientTest {
    private val endpoint = "https://service.example/v1/identify"
    private val photo = byteArrayOf(-1, -40, -1, -39)
    private val result = """{"group":"Copper","confidence":"low","observations":["Reddish surface"],"checks":["Confirm with yard"],"caution":"Photo cannot prove alloy."}"""

    // Production still validates HTTPS. Only the injected connection uses loopback.
    private fun server(block: (HttpServer, AiHttpClient) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val client = AiHttpClient { uri ->
            assertEquals(endpoint, uri.toString())
            URL("http://127.0.0.1:${server.address.port}/v1/identify").openConnection() as HttpURLConnection
        }
        server.start()
        try { block(server, client) } finally { server.stop(0) }
    }

    @Test fun sendsExactPhotoAndHeadersThenParsesResult() = server { server, client ->
        var method = ""; var contentType: String? = null; var accept: String? = null
        var length: String? = null; var body = byteArrayOf()
        server.createContext("/v1/identify") { exchange ->
            method = exchange.requestMethod
            contentType = exchange.requestHeaders.getFirst("Content-Type")
            accept = exchange.requestHeaders.getFirst("Accept")
            length = exchange.requestHeaders.getFirst("Content-Length")
            body = exchange.requestBody.use { it.readBytes() }
            val bytes = result.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        assertEquals("Copper", client.identify(photo, endpoint).group)
        assertEquals("POST", method); assertEquals("image/jpeg", contentType)
        assertEquals("application/json", accept); assertEquals("4", length)
        assertArrayEquals(photo, body)
    }

    @Test fun redirectDoesNotResendPhoto() = server { server, client ->
        val redirects = AtomicInteger()
        server.createContext("/v1/identify") { exchange ->
            exchange.requestBody.close()
            exchange.responseHeaders.add("Location", "/redirected")
            exchange.sendResponseHeaders(307, -1); exchange.close()
        }
        server.createContext("/redirected") { exchange ->
            redirects.incrementAndGet(); exchange.sendResponseHeaders(200, -1); exchange.close()
        }
        assertTrue(runCatching { client.identify(photo, endpoint) }.isFailure)
        assertEquals(0, redirects.get())
    }

    @Test fun oversizedAndMalformedResponsesAreRejectedOverHttp() = server { server, client ->
        var response = "x".repeat(16_385)
        server.createContext("/v1/identify") { exchange ->
            exchange.requestBody.close()
            val bytes = response.toByteArray()
            exchange.sendResponseHeaders(200, 0) // Chunked: no declared length to trust.
            exchange.responseBody.use { it.write(bytes) }; exchange.close()
        }
        assertTrue(runCatching { client.identify(photo, endpoint) }.exceptionOrNull()!!.message!!.contains("too large"))
        response = "{}"
        assertTrue(runCatching { client.identify(photo, endpoint) }.isFailure)
    }

    @Test fun unsafeEndpointInvalidSizeAndCancelledRequestNeverOpenConnection() {
        var opens = 0
        val client = AiHttpClient { opens++; error("Unexpected connection") }
        assertTrue(runCatching { client.identify(photo, "http://service.example/v1/identify") }.isFailure)
        assertTrue(runCatching { client.identify(byteArrayOf(), endpoint) }.isFailure)
        val cancellation = RequestCancellation().apply { cancel() }
        assertTrue(runCatching { client.identify(photo, endpoint, cancellation) }.exceptionOrNull() is CancellationException)
        assertEquals(0, opens)
    }

    @Test fun cancellationReleasesRealHttpWorkerWaitingForResponseHeaders() = server { server, client ->
        val received = CountDownLatch(1)
        val release = CountDownLatch(1)
        val cancellation = RequestCancellation()
        val worker = Executors.newSingleThreadExecutor()
        server.createContext("/v1/identify") { exchange ->
            try {
                exchange.requestBody.use { it.readBytes() }
                received.countDown()
                release.await(5, TimeUnit.SECONDS)
            } finally { exchange.close() }
        }
        try {
            val pending = worker.submit<Boolean> {
                runCatching { client.identify(photo, endpoint, cancellation) }.isFailure
            }
            assertTrue("Request never reached server", received.await(3, TimeUnit.SECONDS))
            cancellation.cancel()
            assertTrue("Cancelled request returned a result", pending.get(3, TimeUnit.SECONDS))
            // Server is still withholding headers: disconnect, not a server reply,
            // released the actual JVM socket worker.
            assertEquals(1L, release.count)
        } finally {
            release.countDown(); cancellation.cancel(); worker.shutdownNow()
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    private open inner class Connection : HttpURLConnection(URL(endpoint)) {
        var disconnects = 0
        var responses = 0
        var reads = 0
        override fun disconnect() { disconnects++ }
        override fun connect() {}
        override fun usingProxy() = false
        override fun getOutputStream(): OutputStream = ByteArrayOutputStream()
        override fun getResponseCode(): Int { responses++; return 200 }
        override fun getInputStream(): InputStream { reads++; return ByteArrayInputStream(result.toByteArray()) }
    }

    @Test fun cancellationAtEndOfUploadStopsResponseWait() {
        val cancellation = RequestCancellation()
        val connection = object : Connection() {
            override fun getOutputStream(): OutputStream = object : ByteArrayOutputStream() {
                override fun close() { cancellation.cancel(); super.close() }
            }
        }
        val failure = runCatching { AiHttpClient { connection }.identify(photo, endpoint, cancellation) }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertEquals(0, connection.responses)
        assertTrue(connection.disconnects >= 1)
    }

    @Test fun throwingCleanupPreservesSuccessfulResultAndDetachesCancellation() {
        val cancellation=RequestCancellation()
        val connection=object : Connection() {
            override fun disconnect() { super.disconnect();throw IllegalStateException("Cleanup failed") }
        }
        assertEquals("Copper",AiHttpClient { connection }.identify(photo,endpoint,cancellation).group)
        cancellation.cancel()
        assertEquals(1,connection.disconnects)
    }

    @Test fun throwingCleanupPreservesOriginalRequestFailure() {
        val connection=object : Connection() {
            override fun getResponseCode()=503
            override fun disconnect() { super.disconnect();throw IllegalStateException("Cleanup failed") }
        }
        val failure=runCatching { AiHttpClient { connection }.identify(photo,endpoint) }.exceptionOrNull()!!
        assertTrue(failure.message!!.contains("AI service is unavailable"))
        assertEquals(1,connection.disconnects)
    }

    @Test fun throwingCleanupPreservesUserCancellation() {
        val cancellation=RequestCancellation()
        val connection=object : Connection() {
            override fun getResponseCode(): Int { cancellation.cancel();return 200 }
            override fun disconnect() { super.disconnect();throw IllegalStateException("Cleanup failed") }
        }
        assertTrue(runCatching { AiHttpClient { connection }.identify(photo,endpoint,cancellation) }
            .exceptionOrNull() is CancellationException)
        assertEquals(0,connection.reads)
    }

    @Test fun cancellationAfterHeadersStopsBodyReadEvenWhenDisconnectDoesNothing() {
        val cancellation = RequestCancellation()
        val connection = object : Connection() {
            override fun getResponseCode(): Int { cancellation.cancel(); return 200 }
            override fun getInputStream(): InputStream = object : InputStream() {
                override fun read(): Int { fail("Cancelled response body was read"); return -1 }
            }
        }
        assertTrue(runCatching { AiHttpClient { connection }.identify(photo, endpoint, cancellation) }
            .exceptionOrNull() is CancellationException)
    }

    @Test fun statusesDoNotReadOrExposeErrorBodyAndAlwaysDisconnect() {
        for ((status, message) in listOf(408 to "Photo upload took too long", 429 to "limit reached", 503 to "unavailable", 500 to "could not finish")) {
            val connection = object : Connection() { override fun getResponseCode() = status }
            val error = runCatching { AiHttpClient { connection }.identify(photo, endpoint) }.exceptionOrNull()!!
            assertTrue(error.message!!.contains(message))
            assertEquals(0, connection.reads); assertEquals(1, connection.disconnects)
        }
    }

    @Test fun timeoutHasActionableMessageAndCompletedRequestDetaches() {
        val cancellation = RequestCancellation()
        val connection = object : Connection() { override fun getResponseCode(): Int = throw SocketTimeoutException("internal host detail") }
        val error = runCatching { AiHttpClient { connection }.identify(photo, endpoint, cancellation) }.exceptionOrNull()!!
        assertTrue(error is SocketTimeoutException)
        assertTrue(error.message!!.contains("Retry or use the manual helper"))
        cancellation.cancel()
        assertEquals(1, connection.disconnects)
    }

    @Test(timeout = 5000) fun overallDeadlineReleasesBlockedUploadAndExplainsTimeout() {
        val released = CountDownLatch(1)
        val connection = object : Connection() {
            override fun disconnect() { super.disconnect(); released.countDown() }
            override fun getOutputStream(): OutputStream = object : OutputStream() {
                override fun write(value: Int) {
                    if (!released.await(3, TimeUnit.SECONDS)) error("Watchdog did not release upload")
                    throw IOException("Socket closed")
                }
            }
        }
        val cancellation = RequestCancellation()
        val failure = runCatching { AiHttpClient(100) { connection }.identify(photo, endpoint, cancellation) }.exceptionOrNull()
        assertTrue("Expected a timeout, got $failure", failure is SocketTimeoutException)
        assertTrue(failure!!.message!!.contains("Retry or use the manual helper"))
        assertTrue(failure.cause is IOException)
        assertEquals(0, connection.responses)
        val closes = connection.disconnects
        cancellation.cancel()
        assertEquals(closes, connection.disconnects)
    }

    @Test(timeout = 5000) fun overallDeadlineDisconnectsRealHttpWhileServerWithholdsHeaders() = server { server, _ ->
        val received = CountDownLatch(1)
        val release = CountDownLatch(1)
        server.createContext("/v1/identify") { exchange ->
            try {
                exchange.requestBody.use { it.readBytes() }; received.countDown()
                release.await(3, TimeUnit.SECONDS)
            } finally { exchange.close() }
        }
        val client = AiHttpClient(250) {
            URL("http://127.0.0.1:${server.address.port}/v1/identify").openConnection() as HttpURLConnection
        }
        try {
            val failure = runCatching { client.identify(photo, endpoint) }.exceptionOrNull()
            assertEquals(0L, received.count)
            assertTrue("Expected deadline timeout, got $failure", failure is SocketTimeoutException)
            assertEquals(1L, release.count)
        } finally { release.countDown() }
    }

    @Test(timeout = 5000) fun deadlineRejectsBodyFromAdapterThatIgnoresDisconnect() {
        val expired = CountDownLatch(1)
        val connection = object : Connection() {
            override fun disconnect() { super.disconnect(); expired.countDown() }
            override fun getInputStream(): InputStream = object : ByteArrayInputStream(result.toByteArray()) {
                override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                    assertTrue(expired.await(3, TimeUnit.SECONDS))
                    return super.read(bytes, offset, length)
                }
            }
        }
        assertTrue(runCatching { AiHttpClient(100) { connection }.identify(photo, endpoint) }
            .exceptionOrNull() is SocketTimeoutException)
    }
}
