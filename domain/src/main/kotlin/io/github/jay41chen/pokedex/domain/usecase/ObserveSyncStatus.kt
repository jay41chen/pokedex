package io.github.jay41chen.pokedex.domain.usecase

import io.github.jay41chen.pokedex.domain.model.SyncStatus
import io.github.jay41chen.pokedex.domain.repository.SyncRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveSyncStatus(private val repository: SyncRepository) {
    operator fun invoke(): StateFlow<SyncStatus> = repository.status
}
