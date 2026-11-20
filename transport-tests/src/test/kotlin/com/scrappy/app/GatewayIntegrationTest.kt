package com.scrappy.app

import com.scrappy.app.data.AiHttpClient
import com.scrappy.app.data.RequestCancellation
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

/** Shared production client -> real Node gateway -> controlled provider. */
class GatewayIntegrationTest {
    private val endpoint="https://service.example/v1/identify"
    private val photo=byteArrayOf(-1,-40,-1,-39)

    private fun gateway(mode: String, block: (AiHttpClient, (Int) -> AiHttpClient, () -> String?) -> Unit) {
        val root=File(System.getProperty("scrappy.projectRoot", ".")).canonicalFile
        val process=ProcessBuilder("node",File(root,"transport-tests/fixtures/gateway.mjs").path,mode)
            .redirectError(ProcessBuilder.Redirect.INHERIT).start()
        val reader=process.inputStream.bufferedReader()
        val io=Executors.newSingleThreadExecutor()
        fun line(): String? = io.submit<String?> { reader.readLine() }.get(5,TimeUnit.SECONDS)
        try {
            val port=line()?.toIntOrNull() ?: error("Gateway did not report a port")
            fun client(timeout: Int)=AiHttpClient(timeout) { uri ->
                assertEquals(endpoint,uri.toString())
                URL("http://127.0.0.1:$port/v1/identify").openConnection() as HttpURLConnection
            }
            block(client(5000),::client,::line)
        } finally {
            process.destroy()
            if(!process.waitFor(2,TimeUnit.SECONDS)) {
                process.destroyForcibly();process.waitFor(2,TimeUnit.SECONDS)
            }
            io.shutdownNow();reader.close()
            assertTrue("Gateway output worker did not stop",io.awaitTermination(2,TimeUnit.SECONDS))
        }
    }

    @Test(timeout=15000) fun photoPassesThroughClientAndGatewayToValidatedResult() = gateway("success") { client,_,_ ->
        val result=client.identify(photo,endpoint)
        assertEquals("Copper",result.group)
        assertEquals("low",result.confidence)
        assertEquals(listOf("Confirm with yard"),result.checks)
    }

    @Test(timeout=15000) fun invalidProviderResultIsHiddenAndNextPhotoSucceeds() = gateway("invalid") { client,_,_ ->
        val failure=runCatching { client.identify(photo,endpoint) }.exceptionOrNull()
        assertEquals("AI service is unavailable. Use the manual helper and try again later.",failure?.message)
        assertEquals("Copper",client.identify(photo,endpoint).group)
    }

    @Test(timeout=15000) fun cancellingClientAbortsGatewayProviderAndAllowsRetry() = gateway("stall") { client,_,line ->
        val cancellation=RequestCancellation()
        val worker=Executors.newSingleThreadExecutor()
        try {
            val pending=worker.submit<Throwable?> {
                runCatching { client.identify(photo,endpoint,cancellation) }.exceptionOrNull()
            }
            assertEquals("START",line())
            cancellation.cancel()
            assertTrue(pending.get(3,TimeUnit.SECONDS) is CancellationException)
            assertEquals("ABORT",line())
            assertEquals("Copper",client.identify(photo,endpoint).group)
        } finally {
            cancellation.cancel();worker.shutdownNow()
            assertTrue(worker.awaitTermination(3,TimeUnit.SECONDS))
        }
    }

    @Test(timeout=15000) fun clientDeadlineAbortsGatewayProviderAndAllowsRetry() = gateway("stall") { client,withTimeout,line ->
        val worker=Executors.newSingleThreadExecutor()
        try {
            val pending=worker.submit<Throwable?> {
                runCatching { withTimeout(1000).identify(photo,endpoint) }.exceptionOrNull()
            }
            assertEquals("START",line())
            assertTrue(pending.get(3,TimeUnit.SECONDS) is SocketTimeoutException)
            assertEquals("ABORT",line())
            assertEquals("Copper",client.identify(photo,endpoint).group)
        } finally {
            worker.shutdownNow();assertTrue(worker.awaitTermination(3,TimeUnit.SECONDS))
        }
    }
}
