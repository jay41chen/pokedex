package io.github.jay41chen.pokedex.data.local.dao

import androidx.room.ColumnInfo

internal data class CaptureRow(
    @ColumnInfo(name = "capture_id") val captureId: String,
    @ColumnInfo(name = "captured_at") val capturedAt: Long,
    @ColumnInfo(name = "pokemon_id") val pokemonId: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
)
