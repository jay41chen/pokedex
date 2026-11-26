package io.github.jay41chen.pokedex.data.sync

/**
 * Called when a request fails permanently (e.g. 404, malformed body). The real implementation
 * (a later stage) logs it and removes that URL from the OkHttp Cache.
 */
internal fun interface PermanentFailureReporter {
    fun report(url: String, cause: Throwable)
}
