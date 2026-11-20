package com.scrappy.app.data

import java.net.HttpURLConnection
import java.net.URI
import java.net.SocketTimeoutException

/** Shared photo transport, independent of Android bitmap preparation. */
class AiHttpClient internal constructor(
    private val requestTimeoutMillis: Int,
    private val openConnection: (URI) -> HttpURLConnection
) {
    internal constructor(openConnection: (URI) -> HttpURLConnection) : this(60_000, openConnection)
    constructor() : this(60_000, { it.toURL().openConnection() as HttpURLConnection })

    init { require(requestTimeoutMillis > 0) }

    fun identify(jpeg: ByteArray, endpoint: String, cancellation: RequestCancellation = RequestCancellation()): AiIdentification {
        cancellation.check()
        val uri=AiIdentificationCodec.endpoint(endpoint)
        require(jpeg.size in 4..1_048_576)
        val connection=openConnection(uri)
        val requestDeadline=RequestDeadline(requestTimeoutMillis) { connection.disconnect() }
        try {
            cancellation.attach { connection.disconnect() }
            cancellation.check()
            connection.requestMethod="POST";connection.instanceFollowRedirects=false
            connection.connectTimeout=15_000;connection.readTimeout=45_000
            connection.doOutput=true;connection.setFixedLengthStreamingMode(jpeg.size)
            connection.setRequestProperty("Content-Type","image/jpeg")
            connection.setRequestProperty("Accept","application/json")
            connection.outputStream.use { it.write(jpeg) }
            cancellation.check()
            requestDeadline.check()
            // Bound the entire response wait, even if the service drips bytes
            // frequently enough to keep an ordinary per-read timeout alive.
            val deadline=ResponseDeadline(45_000)
            connection.readTimeout=deadline.remainingMillis()
            when(connection.responseCode) {
                200 -> Unit
                408 -> error("Photo upload took too long. Retry on a stronger connection or use the manual helper.")
                429 -> error("AI request limit reached. Try again later or use the manual helper.")
                503 -> error("AI service is unavailable. Use the manual helper and try again later.")
                else -> error("AI analysis could not finish. Use the manual helper or retry.")
            }
            cancellation.check()
            requestDeadline.check()
            val bytes=connection.inputStream.use { input ->
                BoundedResponse.read(input, 16_384) {
                    cancellation.check()
                    requestDeadline.check()
                    connection.readTimeout=deadline.remainingMillis()
                }
            }
            cancellation.check()
            val result=AiIdentificationCodec.parse(String(bytes,Charsets.UTF_8))
            requestDeadline.check()
            return result
        } catch (error: Exception) {
            // A watchdog disconnect may surface as IOException, not SocketTimeoutException.
            // Preserve user cancellation; otherwise expose an actionable timeout message.
            cancellation.check()
            try { requestDeadline.check() } catch (timeout: SocketTimeoutException) {
                throw timeout.apply { initCause(error) }
            }
            if (error !is SocketTimeoutException) throw error
            throw SocketTimeoutException("AI analysis took too long. Retry or use the manual helper.").apply { initCause(error) }
        } finally {
            requestDeadline.close()
            cancellation.detach()
            // Preserve the result or original request failure when cleanup fails.
            try { connection.disconnect() } catch (_: Exception) { }
        }
    }
}
