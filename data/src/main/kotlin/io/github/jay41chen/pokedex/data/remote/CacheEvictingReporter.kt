package io.github.jay41chen.pokedex.data.remote

import io.github.jay41chen.pokedex.data.logging.DataLogger
import io.github.jay41chen.pokedex.data.sync.PermanentFailureReporter
import okhttp3.Cache

internal class CacheEvictingReporter(
    private val cache: Cache,
    private val logger: DataLogger,
) : PermanentFailureReporter {

    override fun report(url: String, cause: Throwable) {
        logger.warn("Permanent failure: $url", cause)
        try {
            val it = cache.urls()
            while (it.hasNext()) {
                if (it.next() == url) {
                    it.remove()
                    break
                }
            }
        } catch (e: Exception) {
            logger.warn("Cache eviction failed: $url", e)
        }
    }
}
