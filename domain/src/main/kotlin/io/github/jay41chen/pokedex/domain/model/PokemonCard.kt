package io.github.jay41chen.pokedex.domain.model

// 列表、My Pocket、Evolves from 共用
data class PokemonCard(
    val id: PokemonId,
    val name: String,
    val imageUrl: String?,      // null = 這隻的 detail 還沒抓到
)
