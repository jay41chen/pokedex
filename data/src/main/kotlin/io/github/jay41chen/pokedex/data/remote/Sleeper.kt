package io.github.jay41chen.pokedex.data.remote

/** Wait used by [RetryInterceptor]; tests record the requested time instead of really waiting. */
internal fun interface Sleeper {
    fun sleep(millis: Long)
}

internal object ThreadSleeper : Sleeper {
    override fun sleep(millis: Long) = Thread.sleep(millis)
}
