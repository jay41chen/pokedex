package io.github.jay41chen.pokedex.domain.usecase

import io.github.jay41chen.pokedex.domain.model.CaptureId
import io.github.jay41chen.pokedex.domain.repository.CaptureRepository

class ReleaseCapture(private val repository: CaptureRepository) {
    suspend operator fun invoke(captureId: CaptureId) = repository.release(captureId)
}
