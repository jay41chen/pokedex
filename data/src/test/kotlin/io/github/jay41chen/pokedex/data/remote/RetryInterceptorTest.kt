package io.github.jay41chen.pokedex.data.remote

import java.io.IOException
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RetryInterceptorTest {

    private class TrackingBody : ResponseBody() {
        var closed = false
        override fun contentType() = "text/plain".toMediaType()
        override fun contentLength() = 0L
        override fun source(): BufferedSource = Buffer()
        override fun close() {
            closed = true
        }
    }

    private sealed interface Step {
        data class Reply(val code: Int, val retryAfter: String? = null) : Step
        object Fail : Step
    }

    private class Scripted(private val steps: List<Step>) : Interceptor {
        var calls = 0
        val bodies = mutableListOf<TrackingBody>()

        override fun intercept(chain: Interceptor.Chain): Response {
            val step = steps[calls]
            calls++
            return when (step) {
                Step.Fail -> throw IOException("boom")
                is Step.Reply -> {
                    val body = TrackingBody()
                    bodies += body
                    val b = Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(step.code)
                        .message("m")
                        .body(body)
                    step.retryAfter?.let { b.header("Retry-After", it) }
                    b.build()
                }
            }
        }
    }

    private class FakeSleeper(var onSleep: () -> Unit = {}) : Sleeper {
        val sleeps = mutableListOf<Long>()
        override fun sleep(millis: Long) {
            sleeps += millis
            onSleep()
        }
    }

    private class Fixture(steps: List<Step>, val sleeper: FakeSleeper = FakeSleeper()) {
        val responder = Scripted(steps)
        val client = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(sleeper))
            .addInterceptor(responder)
            .build()

        fun newCall(): Call = client.newCall(Request.Builder().url("http://localhost/test").build())
    }

    private fun ok(code: Int) = Step.Reply(code)

    @Test
    fun `I1 503 503 200 succeeds after two jittered waits and closes failed bodies`() {
        val f = Fixture(listOf(ok(503), ok(503), ok(200)))
        f.newCall().execute().use { r ->
            assertEquals(200, r.code)
        }
        assertEquals(3, f.responder.calls)
        assertEquals(2, f.sleeper.sleeps.size)
        assertTrue(f.sleeper.sleeps[0] in 500..1500)
        assertTrue(f.sleeper.sleeps[1] in 1000..3000)
        assertTrue(f.responder.bodies[0].closed)
        assertTrue(f.responder.bodies[1].closed)
    }

    @Test
    fun `I2 503 four times returns 503 after four attempts`() {
        val f = Fixture(List(4) { ok(503) })
        f.newCall().execute().use { r ->
            assertEquals(503, r.code)
            assertFalse(f.responder.bodies[3].closed)
        }
        assertEquals(4, f.responder.calls)
        assertEquals(3, f.sleeper.sleeps.size)
        assertTrue(f.sleeper.sleeps[0] in 500..1500)
        assertTrue(f.sleeper.sleeps[1] in 1000..3000)
        assertTrue(f.sleeper.sleeps[2] in 2000..5999)
        assertTrue(f.responder.bodies.take(3).all { it.closed })
    }

    @Test
    fun `I3 404 is not retried`() {
        val f = Fixture(listOf(ok(404)))
        f.newCall().execute().use { r ->
            assertEquals(404, r.code)
        }
        assertEquals(1, f.responder.calls)
        assertEquals(0, f.sleeper.sleeps.size)
    }

    @Test
    fun `I4a 429 with Retry-After 3 waits 3000 then retries`() {
        val f = Fixture(listOf(Step.Reply(429, "3"), ok(200)))
        f.newCall().execute().use { r ->
            assertEquals(200, r.code)
        }
        assertEquals(2, f.responder.calls)
        assertEquals(listOf(3000L), f.sleeper.sleeps)
        assertTrue(f.responder.bodies[0].closed)
    }

    @Test
    fun `I4b 429 with Retry-After 30 is returned without waiting`() {
        val f = Fixture(listOf(Step.Reply(429, "30")))
        f.newCall().execute().use { r ->
            assertEquals(429, r.code)
            assertFalse(f.responder.bodies[0].closed)
        }
        assertEquals(1, f.responder.calls)
        assertEquals(0, f.sleeper.sleeps.size)
    }

    @Test
    fun `I4d 429 with Retry-After 5 waits 5000 then retries`() {
        val f = Fixture(listOf(Step.Reply(429, "5"), ok(200)))
        f.newCall().execute().use { r ->
            assertEquals(200, r.code)
        }
        assertEquals(2, f.responder.calls)
        assertEquals(listOf(5000L), f.sleeper.sleeps)
    }

    @Test
    fun `I4e 429 with Retry-After 6 is returned without waiting`() {
        val f = Fixture(listOf(Step.Reply(429, "6")))
        f.newCall().execute().use { r ->
            assertEquals(429, r.code)
            assertFalse(f.responder.bodies[0].closed)
        }
        assertEquals(1, f.responder.calls)
        assertEquals(0, f.sleeper.sleeps.size)
    }

    @Test
    fun `IOException four times throws after four attempts`() {
        val f = Fixture(List(4) { Step.Fail })
        try {
            f.newCall().execute().close()
            fail("expected IOException")
        } catch (_: IOException) {
        }
        assertEquals(4, f.responder.calls)
        assertEquals(3, f.sleeper.sleeps.size)
    }

    @Test
    fun `non-standard code 600 is not retried`() {
        val f = Fixture(listOf(ok(600)))
        f.newCall().execute().use { r ->
            assertEquals(600, r.code)
        }
        assertEquals(1, f.responder.calls)
        assertEquals(0, f.sleeper.sleeps.size)
    }

    @Test
    fun `IOException then 200 retries once`() {
        val f = Fixture(listOf(Step.Fail, ok(200)))
        f.newCall().execute().use { r ->
            assertEquals(200, r.code)
        }
        assertEquals(2, f.responder.calls)
        assertEquals(1, f.sleeper.sleeps.size)
        assertTrue(f.sleeper.sleeps[0] in 500..1500)
    }

    @Test
    fun `cancel during wait throws and does not send another request`() {
        val sleeper = FakeSleeper()
        val f = Fixture(listOf(ok(503), ok(200)), sleeper)
        val call = f.newCall()
        sleeper.onSleep = { call.cancel() }
        try {
            call.execute().close()
            fail("expected IOException")
        } catch (_: IOException) {
        }
        assertEquals(1, f.responder.calls)
    }

    @Test
    fun `429 without Retry-After uses normal backoff`() {
        val f = Fixture(listOf(ok(429), ok(200)))
        f.newCall().execute().use { r ->
            assertEquals(200, r.code)
        }
        assertEquals(1, f.sleeper.sleeps.size)
        assertTrue(f.sleeper.sleeps[0] in 500..1500)
    }

    @Test
    fun `I4c 429 with negative Retry-After uses normal backoff`() {
        val f = Fixture(listOf(Step.Reply(429, "-1"), ok(200)))
        f.newCall().execute().use { r ->
            assertEquals(200, r.code)
        }
        assertEquals(2, f.responder.calls)
        assertEquals(1, f.sleeper.sleeps.size)
        assertTrue(f.sleeper.sleeps[0] in 500..1500)
        assertTrue(f.responder.bodies[0].closed)
    }
}
