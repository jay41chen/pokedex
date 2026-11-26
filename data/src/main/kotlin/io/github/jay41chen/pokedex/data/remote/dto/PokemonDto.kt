package io.github.jay41chen.pokedex.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PokemonDto(
    val sprites: Sprites? = null,
    val types: List<TypeSlot>,
) {
    @Serializable
    data class Sprites(val other: Other? = null)

    @Serializable
    data class Other(
        @SerialName("official-artwork") val officialArtwork: Artwork? = null,
    )

    @Serializable
    data class Artwork(
        @SerialName("front_default") val frontDefault: String? = null,
    )

    @Serializable
    data class TypeSlot(val slot: Int, val type: TypeRef)

    @Serializable
    data class TypeRef(val name: String)
}
