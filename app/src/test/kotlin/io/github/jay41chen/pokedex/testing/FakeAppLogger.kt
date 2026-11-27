package io.github.jay41chen.pokedex.testing

import io.github.jay41chen.pokedex.logging.AppLogger

class FakeAppLogger : AppLogger {
    val warnings = mutableListOf<Pair<String, Throwable?>>()

    override fun warn(message: String, cause: Throwable?) {
        warnings += message to cause
    }
}
