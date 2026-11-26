package io.github.jay41chen.pokedex.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jay41chen.pokedex.domain.model.SyncResult
import io.github.jay41chen.pokedex.domain.model.SyncStatus
import io.github.jay41chen.pokedex.domain.model.TypeSection
import io.github.jay41chen.pokedex.domain.usecase.CapturePokemon
import io.github.jay41chen.pokedex.domain.usecase.ObserveCaptures
import io.github.jay41chen.pokedex.domain.usecase.ObserveSyncStatus
import io.github.jay41chen.pokedex.domain.usecase.ObserveTypeSections
import io.github.jay41chen.pokedex.domain.usecase.ReleaseCapture
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 尚未加 @HiltViewModel：repo binding 還不存在，Hilt 整合階段再加。
class HomeViewModel @Inject constructor(
    observeTypeSections: ObserveTypeSections,
    observeCaptures: ObserveCaptures,
    observeSyncStatus: ObserveSyncStatus,
    private val capturePokemon: CapturePokemon,
    private val releaseCapture: ReleaseCapture,
) : ViewModel() {

    private data class State(
        val ui: HomeUiState,
        val lastShownRound: Long = 0L,
        val lastFinished: SyncResult? = null,
    )

    private val internal = MutableStateFlow(
        State(
            ui = HomeUiState(
                pocket = emptyList(),
                sections = emptyList(),
                catalog = CatalogStatus.Loading,
                message = null,
            ),
        ),
    )

    val state: StateFlow<HomeUiState> =
        internal.map { it.ui }.stateIn(viewModelScope, SharingStarted.Eagerly, internal.value.ui)

    init {
        viewModelScope.launch {
            observeTypeSections().collect { sections -> internal.update { it.withSections(sections) } }
        }
        viewModelScope.launch {
            observeCaptures().collect { captures ->
                internal.update { it.copy(ui = it.ui.copy(pocket = captures)) }
            }
        }
        viewModelScope.launch {
            observeSyncStatus().collect { status -> internal.update { it.onStatus(status) } }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Capture -> viewModelScope.launch { capturePokemon(intent.id) }
            is HomeIntent.Release -> viewModelScope.launch { releaseCapture(intent.captureId) }
            HomeIntent.MessageShown -> internal.update { it.copy(ui = it.ui.copy(message = null)) }
        }
    }

    private fun State.withSections(sections: List<TypeSection>): State =
        copy(ui = ui.copy(sections = sections)).withCatalog()

    private fun State.onStatus(status: SyncStatus): State {
        if (status !is SyncStatus.Finished || status.round <= lastShownRound) return this
        val message = when (status.result) {
            SyncResult.Offline -> UserMessage.Offline
            SyncResult.PartialFailure -> UserMessage.PartialFailure
            SyncResult.Success -> null
        }
        return copy(
            ui = ui.copy(message = message),
            lastShownRound = status.round,
            lastFinished = status.result,
        ).withCatalog()
    }

    private fun State.withCatalog(): State = copy(ui = ui.copy(catalog = deriveCatalog(ui.sections, lastFinished)))

    private fun deriveCatalog(sections: List<TypeSection>, lastFinished: SyncResult?): CatalogStatus = when {
        sections.isNotEmpty() -> CatalogStatus.Ready
        lastFinished == null -> CatalogStatus.Loading
        else -> CatalogStatus.Empty(offline = lastFinished == SyncResult.Offline)
    }
}
