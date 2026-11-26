package io.github.jay41chen.pokedex.ui.home

import io.github.jay41chen.pokedex.domain.model.CaptureId
import io.github.jay41chen.pokedex.domain.model.CapturedPokemon
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.TypeSection

data class HomeUiState(
    val pocket: List<CapturedPokemon>,
    val sections: List<TypeSection>,
    val catalog: CatalogStatus,
    val message: UserMessage?,            // null = 沒有要顯示的 Snackbar
)

sealed interface CatalogStatus {
    data object Loading : CatalogStatus   // DB 空，且 VM 還沒看過任何 Finished
    data object Ready : CatalogStatus     // 至少一隻
    data class Empty(val offline: Boolean) : CatalogStatus  // DB 空，且 VM 看過 Finished；offline = 最近一次 Finished 的 result == Offline（之後 Running 期間維持，不退回 Loading）
}

enum class UserMessage { Offline, PartialFailure }

sealed interface HomeIntent {
    data class Capture(val id: PokemonId) : HomeIntent
    data class Release(val captureId: CaptureId) : HomeIntent
    data object MessageShown : HomeIntent  // UI 顯示完 Snackbar 後回報，VM 把 message 清成 null
}
