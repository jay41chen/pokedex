package io.github.jay41chen.pokedex.data.mapper

// internal: only used inside :data; the Hilt module provides domain interfaces only.

import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonSpeciesEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonTypeEntity
import io.github.jay41chen.pokedex.data.remote.dto.PokemonDto
import io.github.jay41chen.pokedex.data.remote.dto.PokemonListDto
import io.github.jay41chen.pokedex.data.remote.dto.PokemonSpeciesDto

internal fun idFromUrl(url: String): Int =
    url.trimEnd('/').substringAfterLast('/').toIntOrNull()
        ?: throw InvalidDataException("Cannot extract id from url: $url")

internal fun PokemonListDto.toEntities(): List<PokemonEntity> =
    results.map { PokemonEntity(id = idFromUrl(it.url), name = it.name) }

internal fun PokemonDto.toEntities(pokemonId: Int): Pair<PokemonDetailEntity, List<PokemonTypeEntity>> {
    if (types.isEmpty()) throw InvalidDataException("Pokemon $pokemonId has no types")
    val detail = PokemonDetailEntity(
        pokemonId = pokemonId,
        imageUrl = sprites?.other?.officialArtwork?.frontDefault,
    )
    val typeEntities = types.sortedBy { it.slot }
        .map { PokemonTypeEntity(pokemonId, it.slot, it.type.name) }
    return detail to typeEntities
}

internal fun PokemonSpeciesDto.toEntity(pokemonId: Int): PokemonSpeciesEntity =
    PokemonSpeciesEntity(
        pokemonId = pokemonId,
        evolvesFromId = evolvesFromSpecies?.let { idFromUrl(it.url) }, // no >151 filter on write
        description = flavorTextEntries.firstOrNull { it.language.name == "en" }?.flavorText ?: "",
    )
