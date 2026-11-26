package io.github.jay41chen.pokedex.data.remote

/** HTTP codes treated as transient: retried by RetryInterceptor, classified Transient by sync. */
internal fun isRetryableHttpCode(code: Int): Boolean = code in 500..599 || code == 429
