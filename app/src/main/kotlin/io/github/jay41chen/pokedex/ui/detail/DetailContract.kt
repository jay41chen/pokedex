package io.github.jay41chen.pokedex.ui.detail

import io.github.jay41chen.pokedex.domain.model.PokemonDetail

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Content(val detail: PokemonDetail) : DetailUiState
}
