package io.github.jay41chen.pokedex.domain.model

data class Species(
    val evolvesFrom: PokemonCard?,  // null = 沒有前一階段，或前一階段 > 151
    val description: String,
)
