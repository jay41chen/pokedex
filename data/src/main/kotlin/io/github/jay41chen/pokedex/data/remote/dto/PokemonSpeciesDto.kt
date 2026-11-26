package io.github.jay41chen.pokedex.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PokemonSpeciesDto(
    @SerialName("evolves_from_species") val evolvesFromSpecies: SpeciesRef? = null,
    @SerialName("flavor_text_entries") val flavorTextEntries: List<FlavorText>,
) {
    @Serializable
    data class SpeciesRef(val url: String)

    @Serializable
    data class FlavorText(
        @SerialName("flavor_text") val flavorText: String,
        val language: Language,
    )

    @Serializable
    data class Language(val name: String)
}
