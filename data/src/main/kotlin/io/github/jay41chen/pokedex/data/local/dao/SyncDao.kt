package io.github.jay41chen.pokedex.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonSpeciesEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonTypeEntity

@Dao
internal abstract class SyncDao {
    @Query("SELECT NOT EXISTS (SELECT 1 FROM pokemon)")
    abstract suspend fun isPokemonEmpty(): Boolean

    @Query(
        "SELECT p.id FROM pokemon p LEFT JOIN pokemon_detail d ON d.pokemon_id = p.id " +
            "WHERE d.pokemon_id IS NULL ORDER BY p.id ASC",
    )
    abstract suspend fun pendingDetailIds(): List<Int>

    @Query(
        "SELECT p.id FROM pokemon p LEFT JOIN pokemon_species s ON s.pokemon_id = p.id " +
            "WHERE s.pokemon_id IS NULL ORDER BY p.id ASC",
    )
    abstract suspend fun pendingSpeciesIds(): List<Int>

    @Upsert
    abstract suspend fun upsertPokemon(list: List<PokemonEntity>)

    @Upsert
    abstract suspend fun upsertDetail(detail: PokemonDetailEntity)

    @Upsert
    abstract suspend fun upsertTypes(types: List<PokemonTypeEntity>)

    @Upsert
    abstract suspend fun upsertSpecies(species: PokemonSpeciesEntity)

    @Transaction
    open suspend fun writeDetail(detail: PokemonDetailEntity, types: List<PokemonTypeEntity>) {
        upsertDetail(detail)
        upsertTypes(types)
    }
}
