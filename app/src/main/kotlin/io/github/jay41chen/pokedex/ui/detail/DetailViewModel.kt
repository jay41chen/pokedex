package io.github.jay41chen.pokedex.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.usecase.ObservePokemonDetail
import io.github.jay41chen.pokedex.ui.navigation.DetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted route: DetailRoute,
    observePokemonDetail: ObservePokemonDetail,
) : ViewModel() {

    val state: StateFlow<DetailUiState> =
        observePokemonDetail(PokemonId(route.id))
            .map { detail ->
                if (detail == null) DetailUiState.Loading else DetailUiState.Content(detail)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, DetailUiState.Loading)

    @AssistedFactory
    interface Factory {
        fun create(route: DetailRoute): DetailViewModel
    }
}
