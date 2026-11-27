package io.github.jay41chen.pokedex.testing

import io.github.jay41chen.pokedex.domain.model.CaptureId
import io.github.jay41chen.pokedex.domain.model.CatalogEntry
import io.github.jay41chen.pokedex.domain.model.CapturedPokemon
import io.github.jay41chen.pokedex.domain.model.PokemonDetail
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.SyncStatus
import io.github.jay41chen.pokedex.domain.repository.CaptureRepository
import io.github.jay41chen.pokedex.domain.repository.PokemonRepository
import io.github.jay41chen.pokedex.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePokemonRepository : PokemonRepository {
    val catalog = MutableStateFlow<List<CatalogEntry>>(emptyList())
    var catalogSource: Flow<List<CatalogEntry>>? = null
    private val details = mutableMapOf<PokemonId, MutableStateFlow<PokemonDetail?>>()

    override fun observeCatalog(): Flow<List<CatalogEntry>> = catalogSource ?: catalog

    fun detail(id: PokemonId): MutableStateFlow<PokemonDetail?> = details.getOrPut(id) { MutableStateFlow(null) }

    override fun observeDetail(id: PokemonId): Flow<PokemonDetail?> = detail(id)
}

class FakeCaptureRepository : CaptureRepository {
    val captures = MutableStateFlow<List<CapturedPokemon>>(emptyList())

    override fun observeCaptures(): Flow<List<CapturedPokemon>> = captures
    var captureError: Throwable? = null
    var releaseError: Throwable? = null

    override suspend fun capture(id: PokemonId) {
        captureError?.let { throw it }
    }

    override suspend fun release(captureId: CaptureId) {
        releaseError?.let { throw it }
    }
}

class FakeSyncRepository : SyncRepository {
    override val status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    var requestSyncCount = 0

    override fun requestSync() {
        requestSyncCount++
    }
}
