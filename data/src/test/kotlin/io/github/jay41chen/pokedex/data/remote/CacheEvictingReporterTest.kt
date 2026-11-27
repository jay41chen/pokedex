package io.github.jay41chen.pokedex.data.remote

import io.github.jay41chen.pokedex.data.logging.DataLogger
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.Cache
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CacheEvictingReporterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class RecordingLogger : DataLogger {
        val messages = CopyOnWriteArrayList<String>()
        override fun warn(message: String, cause: Throwable?) {
            messages += message
        }
    }

    @Test
    fun `H2a report on an empty cache does not throw`() {
        val cache = Cache(tmp.newFolder("cache"), 1024L * 1024)
        CacheEvictingReporter(cache, RecordingLogger()).report("https://example.invalid/x", RuntimeException())
        cache.close()
    }

    @Test
    fun `H2b report logs eviction failure and does not throw when cache path is a regular file`() {
        val file: File = tmp.newFile("not-a-dir")
        val cache = Cache(file, 1024L * 1024)
        val logger = RecordingLogger()
        try {
            CacheEvictingReporter(cache, logger).report("https://example.invalid/x", RuntimeException())
            assertTrue(logger.messages.any { it.startsWith("Cache eviction failed") })
        } finally {
            // close may throw because the path is a regular file
            runCatching { cache.close() }
        }
    }
}
