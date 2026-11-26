package io.github.jay41chen.pokedex.data.remote

import java.io.IOException
import kotlin.random.Random
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Application interceptor shared by Retrofit and Coil.
 *
 * Retrofit cancellation only calls `call.cancel()` and does not interrupt the thread, so a worker
 * that is already sleeping sleeps to the end (up to about 6 seconds). The cancel check at the
 * top of the next attempt then stops it, so it never sends the next request.
 */
internal class RetryInterceptor(private val sleeper: Sleeper) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 1
        while (true) {
            checkCanceled(chain)
            val response = try {
                chain.proceed(chain.request())
            } catch (e: IOException) {
                if (chain.call().isCanceled() || attempt >= MAX_ATTEMPTS) throw e
                wait(chain, backoffMillis(attempt))
                attempt++
                continue
            }

            val code = response.code
            val retryable = isRetryableHttpCode(code)
            if (!retryable || attempt >= MAX_ATTEMPTS) return response

            val waitMillis = if (code == 429) {
                val seconds = response.header("Retry-After")?.toLongOrNull()?.takeIf { it >= 0 }
                when {
                    seconds == null -> {
                        // [待確認] 缺 Retry-After、非整數或負數時照一般退避，spec 未規定
                        backoffMillis(attempt)
                    }
                    seconds > MAX_RETRY_AFTER_SECONDS -> return response
                    else -> seconds * 1000
                }
            } else {
                backoffMillis(attempt)
            }
            response.close()
            wait(chain, waitMillis)
            attempt++
        }
    }

    private fun backoffMillis(retryNumber: Int): Long =
        (BACKOFF_MS[retryNumber - 1] * Random.Default.nextDouble(0.5, 1.5)).toLong()

    private fun wait(chain: Interceptor.Chain, millis: Long) {
        checkCanceled(chain)
        sleeper.sleep(millis)
    }

    private fun checkCanceled(chain: Interceptor.Chain) {
        if (chain.call().isCanceled()) throw IOException("Canceled")
    }

    internal companion object {
        const val MAX_ATTEMPTS = 4
        val BACKOFF_MS = longArrayOf(1000, 2000, 4000)
        const val MAX_RETRY_AFTER_SECONDS = 5
    }
}
