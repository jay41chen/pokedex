package io.github.jay41chen.pokedex.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "pokemon_type",
    primaryKeys = ["pokemon_id", "slot"],
    foreignKeys = [
        ForeignKey(
            entity = PokemonDetailEntity::class,
            parentColumns = ["pokemon_id"],
            childColumns = ["pokemon_id"],
        ),
    ],
)
internal data class PokemonTypeEntity(
    @ColumnInfo(name = "pokemon_id") val pokemonId: Int,
    val slot: Int,
    @ColumnInfo(name = "type_name") val typeName: String,
)
