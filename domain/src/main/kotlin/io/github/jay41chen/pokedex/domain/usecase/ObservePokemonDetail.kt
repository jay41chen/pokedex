package io.github.jay41chen.pokedex.domain.usecase

import io.github.jay41chen.pokedex.domain.model.PokemonDetail
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow

class ObservePokemonDetail(private val repository: PokemonRepository) {
    operator fun invoke(id: PokemonId): Flow<PokemonDetail?> = repository.observeDetail(id)
}
