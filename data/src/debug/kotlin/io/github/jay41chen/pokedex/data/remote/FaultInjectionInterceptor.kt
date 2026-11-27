package io.github.jay41chen.pokedex.data.remote

import java.io.File
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Debug only. Replaces the body of responses for chosen URLs with an unparseable `{}`, so that a
 * 200 response with a bad body gets cached and CacheEvictingReporter's eviction on permanent
 * failure can be verified by hand.
 *
 * Must be registered as a NETWORK interceptor so the corrupted body is what OkHttp writes to the
 * Cache.
 *
 * `Content-Encoding` and `Content-Length` are stripped because the original headers describe the
 * original body. Otherwise BridgeInterceptor would try to gunzip "{}" and throw an IOException,
 * which is transient, not permanent.
 *
 * The file is read on every request (one full URL per line), so it can be toggled via
 * `adb shell run-as` without restarting the app.
 */
internal class FaultInjectionInterceptor(private val faultFile: File) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (request.url.toString() !in readFaultUrls()) return response

        val original = response.body
        val corrupted = "{}".toResponseBody(original.contentType())
        original.close()
        return response.newBuilder()
            .removeHeader("Content-Encoding")
            .removeHeader("Content-Length")
            .body(corrupted)
            .build()
    }

    private fun readFaultUrls(): Set<String> =
        try {
            if (faultFile.exists()) {
                faultFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            } else {
                emptySet()
            }
        } catch (_: Exception) {
            emptySet()
        }
}
