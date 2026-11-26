package io.github.jay41chen.pokedex.domain.usecase

import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.repository.CaptureRepository

class CapturePokemon(private val repository: CaptureRepository) {
    suspend operator fun invoke(id: PokemonId) = repository.capture(id)
}
