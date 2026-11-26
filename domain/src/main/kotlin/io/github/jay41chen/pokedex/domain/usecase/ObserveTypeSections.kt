package io.github.jay41chen.pokedex.domain.usecase

import io.github.jay41chen.pokedex.domain.model.TypeSection
import io.github.jay41chen.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveTypeSections(private val repository: PokemonRepository) {
    operator fun invoke(): Flow<List<TypeSection>> = repository.observeCatalog().map(::groupByType)
}
