package io.github.jay41chen.pokedex.ui.component

import kotlin.math.floor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileWidthTest {
    private val rows = listOf(360f, 411f, 600f, 840f, 1280f)

    @Test
    fun `last tile is half visible at every width`() {
        for (row in rows) {
            val w = tileWidthDp(row)
            val n = maxOf(3, floor((row - 16f) / (110f + 8f)).toInt())
            val visibleOfLast = (row - 16f - n * (w + 8f)) / w

            assertEquals("row=$row", 0.5f, visibleOfLast, 0.001f)
            assertTrue("row=$row n=$n", n >= 3)
            assertTrue("row=$row w=$w", w in 85f..125f)
        }
    }

    @Test
    fun `narrow screen still shows at least 3 full tiles`() {
        val w = tileWidthDp(360f)

        // 3 張完整 + 半張 + 間距要剛好填滿可用寬
        assertEquals(360f - 16f, 3 * (w + 8f) + 0.5f * w, 0.001f)
    }
}
