package io.github.jay41chen.pokedex.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "capture",
    foreignKeys = [
        ForeignKey(
            entity = PokemonEntity::class,
            parentColumns = ["id"],
            childColumns = ["pokemon_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("pokemon_id")],
)
internal data class CaptureEntity(
    @PrimaryKey val id: String, // UUID
    @ColumnInfo(name = "pokemon_id") val pokemonId: Int,
    @ColumnInfo(name = "captured_at") val capturedAt: Long,
)
