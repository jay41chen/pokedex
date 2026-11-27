package io.github.jay41chen.pokedex.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileWidthTest {
    // 預期張數手算寫死：(row - 16) / 118 取 floor，最少 3；不用和實作相同的公式
    private val expectedFullTiles = mapOf(
        360f to 3,
        411f to 3,
        600f to 4,
        840f to 6,
        1280f to 10,
    )

    @Test
    fun `last tile is half visible at every width`() {
        for ((row, n) in expectedFullTiles) {
            val w = tileWidthDp(row)
            val visibleOfLast = (row - 16f - n * (w + 8f)) / w

            assertEquals("row=$row", 0.5f, visibleOfLast, 0.001f)
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
