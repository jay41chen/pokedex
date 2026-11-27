package io.github.jay41chen.pokedex.data.logging

import android.util.Log

internal fun interface DataLogger {
    fun warn(message: String, cause: Throwable?)
}

internal object AndroidDataLogger : DataLogger {
    private const val TAG = "PokedexData"

    override fun warn(message: String, cause: Throwable?) {
        Log.w(TAG, message, cause)
    }
}
