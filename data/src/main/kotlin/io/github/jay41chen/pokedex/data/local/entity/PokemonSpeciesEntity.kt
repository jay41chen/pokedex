package io.github.jay41chen.pokedex.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "pokemon_species",
    foreignKeys = [
        ForeignKey(
            entity = PokemonEntity::class,
            parentColumns = ["id"],
            childColumns = ["pokemon_id"],
        ),
    ],
)
internal data class PokemonSpeciesEntity(
    @PrimaryKey @ColumnInfo(name = "pokemon_id") val pokemonId: Int,
    @ColumnInfo(name = "evolves_from_id") val evolvesFromId: Int?, // no FK; may be > 151
    val description: String,
)
