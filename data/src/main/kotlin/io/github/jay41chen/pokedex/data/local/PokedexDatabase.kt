package io.github.jay41chen.pokedex.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.jay41chen.pokedex.data.local.dao.CaptureDao
import io.github.jay41chen.pokedex.data.local.dao.SyncDao
import io.github.jay41chen.pokedex.data.local.entity.CaptureEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonSpeciesEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonTypeEntity

@Database(
    entities = [
        PokemonEntity::class,
        PokemonDetailEntity::class,
        PokemonTypeEntity::class,
        PokemonSpeciesEntity::class,
        CaptureEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class PokedexDatabase : RoomDatabase() {
    abstract fun syncDao(): SyncDao
    abstract fun captureDao(): CaptureDao
}
