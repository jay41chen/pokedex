package io.github.jay41chen.pokedex.domain.model

data class PokemonDetail(
    val card: PokemonCard,
    val types: List<TypeName>,
    val species: Species?,      // null = species 還沒抓到
)
