package io.github.jay41chen.pokedex.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationGuardTest {
    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun `runs action when RESUMED`() {
        val owner = Owner().apply { registry.currentState = Lifecycle.State.RESUMED }
        var count = 0

        owner.lifecycle.runIfResumed { count++ }

        assertEquals(1, count)
    }

    @Test
    fun `drops action when STARTED`() {
        val owner = Owner().apply { registry.currentState = Lifecycle.State.STARTED }
        var count = 0

        owner.lifecycle.runIfResumed { count++ }

        assertEquals(0, count)
    }
}
