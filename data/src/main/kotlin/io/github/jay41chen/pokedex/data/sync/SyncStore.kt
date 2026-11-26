package io.github.jay41chen.pokedex.data.sync

import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonSpeciesEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonTypeEntity

internal interface SyncStore {
    suspend fun pendingIds(): PendingWork
    suspend fun writeList(pokemon: List<PokemonEntity>)
    suspend fun writeDetail(detail: PokemonDetailEntity, types: List<PokemonTypeEntity>)
    suspend fun writeSpecies(species: PokemonSpeciesEntity)
}
