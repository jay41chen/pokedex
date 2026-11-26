package io.github.jay41chen.pokedex.domain.usecase

import io.github.jay41chen.pokedex.domain.model.CapturedPokemon
import io.github.jay41chen.pokedex.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow

class ObserveCaptures(private val repository: CaptureRepository) {
    operator fun invoke(): Flow<List<CapturedPokemon>> = repository.observeCaptures()
}
