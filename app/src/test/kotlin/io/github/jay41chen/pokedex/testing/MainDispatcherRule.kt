package io.github.jay41chen.pokedex.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces `Dispatchers.Main` with an [UnconfinedTestDispatcher] for each test.
 *
 * - `UnconfinedTestDispatcher` runs `viewModelScope` collectors eagerly, so tests can read
 *   `state.value` directly right after changing a fake repository's flow.
 * - If this is switched to `StandardTestDispatcher`, every assertion must first call
 *   `advanceUntilIdle()` so the pending collectors run before reading `state.value`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
