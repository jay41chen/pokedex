package io.github.jay41chen.pokedex.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class PokemonListDto(val results: List<Item>) {
    @Serializable
    data class Item(val name: String, val url: String)
}
