package io.github.jay41chen.pokedex.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * H1: OkHttp callTimeout covers the retries made by an application interceptor.
 * This verifies the OkHttp behavior only; the value DataModule uses (CALL_TIMEOUT_SECONDS) is not tested.
 */
class CallTimeoutTest {

    @Test
    fun `H1 callTimeout stops the retry loop`() {
        val attempts = AtomicInteger(0)
        val shortCircuit = Interceptor { chain ->
            attempts.incrementAndGet()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(503)
                .message("Service Unavailable")
                .body("".toResponseBody("text/plain".toMediaType()))
                .build()
        }
        val client = OkHttpClient.Builder()
            .callTimeout(100, TimeUnit.MILLISECONDS)
            .addInterceptor(RetryInterceptor { Thread.sleep(200) })
            .addInterceptor(shortCircuit)
            .build()

        try {
            client.newCall(Request.Builder().url("http://localhost/x").build()).execute()
            fail("expected IOException")
        } catch (_: IOException) {
            // expected: the timeout fires during the first backoff sleep
        }
        assertEquals(1, attempts.get())
    }
}
