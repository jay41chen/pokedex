package io.github.jay41chen.pokedex.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.usecase.ObservePokemonDetail
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Hilt integration will switch this to assisted injection; the screen passes the id from its route.
class DetailViewModel(
    id: PokemonId,
    observePokemonDetail: ObservePokemonDetail,
) : ViewModel() {

    val state: StateFlow<DetailUiState> =
        observePokemonDetail(id)
            .map { detail ->
                if (detail == null) DetailUiState.Loading else DetailUiState.Content(detail)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, DetailUiState.Loading)
}
