package io.github.jay41chen.pokedex.domain.model

data class CapturedPokemon(
    val captureId: CaptureId,
    val pokemon: PokemonCard,
    val capturedAtEpochMillis: Long,
)
