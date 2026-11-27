package io.github.jay41chen.pokedex.data.remote

import java.io.File
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FaultInjectionInterceptorTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val url = "http://localhost/api/v2/pokemon/25"

    private class Scripted : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response =
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Encoding", "gzip")
                .header("Content-Length", "99")
                .header("Cache-Control", "max-age=86400")
                .body("original".toResponseBody("application/json".toMediaType()))
                .build()
    }

    private fun call(faultFile: File): Response =
        OkHttpClient.Builder()
            .addInterceptor(FaultInjectionInterceptor(faultFile))
            .addInterceptor(Scripted())
            .build()
            .newCall(Request.Builder().url(url).build())
            .execute()

    @Test
    fun `FI1 listed URL gets body replaced and encoding headers stripped`() {
        val file = tmp.newFile("fault_urls.txt").apply { writeText("\n  $url  \n") }
        call(file).use { r ->
            assertEquals("{}", r.body.string())
            assertNull(r.header("Content-Encoding"))
            assertNull(r.header("Content-Length"))
            assertEquals("max-age=86400", r.header("Cache-Control"))
        }
    }

    @Test
    fun `FI2 other URL in file leaves response unchanged`() {
        val file = tmp.newFile("fault_urls.txt").apply { writeText("http://localhost/api/v2/pokemon/1\n") }
        call(file).use { r ->
            assertEquals("original", r.body.string())
            assertEquals("gzip", r.header("Content-Encoding"))
            assertEquals("99", r.header("Content-Length"))
            assertEquals("max-age=86400", r.header("Cache-Control"))
        }
    }

    @Test
    fun `FI3 missing file leaves response unchanged`() {
        call(File(tmp.root, "missing.txt")).use { r ->
            assertEquals("original", r.body.string())
        }
    }
}
