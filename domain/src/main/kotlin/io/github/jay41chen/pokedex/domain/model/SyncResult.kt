package io.github.jay41chen.pokedex.domain.model

sealed interface SyncResult {
    data object Success : SyncResult
    data object PartialFailure : SyncResult  // 有暫時性失敗沒補上；缺的下次回前景會自動補
    data object Offline : SyncResult         // 沒網路，或中途斷線而中止
}
