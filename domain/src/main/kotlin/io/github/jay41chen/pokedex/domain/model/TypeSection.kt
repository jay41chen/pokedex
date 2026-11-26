package io.github.jay41chen.pokedex.domain.model

data class TypeSection(val type: TypeName, val pokemon: List<PokemonCard>)  // 數量 = pokemon.size
