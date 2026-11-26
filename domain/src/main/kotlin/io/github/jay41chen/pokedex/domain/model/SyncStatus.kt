package io.github.jay41chen.pokedex.domain.model

sealed interface SyncStatus {
    data object Idle : SyncStatus                                              // 這個 process 還沒跑過任何一輪
    data object Running : SyncStatus
    data class Finished(val round: Long, val result: SyncResult) : SyncStatus  // round 從 1 開始，每輪加 1
}
