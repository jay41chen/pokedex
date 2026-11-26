package io.github.jay41chen.pokedex.data.sync

internal fun interface ConnectivityChecker {
    fun isOnline(): Boolean
}
