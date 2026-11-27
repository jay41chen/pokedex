package io.github.jay41chen.pokedex.data.mapper

// internal: only used inside :data; the Hilt module provides domain interfaces only.

import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonSpeciesEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonTypeEntity
import io.github.jay41chen.pokedex.domain.model.CatalogEntry
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonDetail
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.PokemonScope
import io.github.jay41chen.pokedex.domain.model.Species
import io.github.jay41chen.pokedex.domain.model.TypeName

private val DISPLAY_NAME_OVERRIDES = mapOf(
    29 to "Nidoran♀",
    32 to "Nidoran♂",
    83 to "Farfetch’d",
    122 to "Mr. Mime",
)

private val WHITESPACE = Regex("\\s+")

internal fun toCard(pokemon: PokemonEntity, detail: PokemonDetailEntity?): PokemonCard =
    PokemonCard(
        id = PokemonId(pokemon.id),
        name = DISPLAY_NAME_OVERRIDES[pokemon.id] ?: pokemon.name.replaceFirstChar { it.uppercase() },
        imageUrl = detail?.imageUrl,
    )

internal fun toSpecies(species: PokemonSpeciesEntity, evolvesFrom: PokemonCard?): Species =
    Species(
        evolvesFrom = evolvesFrom?.takeIf { it.id.value <= PokemonScope.MAX_ID },
        // [待確認] 待對照 mock 實測
        description = species.description
            .replace('\u000c', ' ').replace('\n', ' ').replace('\r', ' ')
            .replace(WHITESPACE, " ")
            .trim(),
    )

internal fun toDetail(card: PokemonCard, types: List<PokemonTypeEntity>, species: Species?): PokemonDetail =
    PokemonDetail(card = card, types = sortedTypeNames(types), species = species)

internal fun toCatalogEntry(card: PokemonCard, types: List<PokemonTypeEntity>): CatalogEntry =
    CatalogEntry(card = card, types = sortedTypeNames(types))

private fun sortedTypeNames(types: List<PokemonTypeEntity>): List<TypeName> =
    types.sortedBy { it.slot }.map { TypeName(it.typeName) }
