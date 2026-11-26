package io.github.jay41chen.pokedex.domain.usecase

import io.github.jay41chen.pokedex.domain.repository.SyncRepository

class RequestSync(private val repository: SyncRepository) {
    operator fun invoke() = repository.requestSync()
}
