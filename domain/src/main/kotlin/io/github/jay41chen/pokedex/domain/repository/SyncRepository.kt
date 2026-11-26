package io.github.jay41chen.pokedex.domain.repository

import io.github.jay41chen.pokedex.domain.model.SyncStatus
import kotlinx.coroutines.flow.StateFlow

interface SyncRepository {
    fun requestSync()                   // 非 suspend；回傳前 status 已是 Running；在 application scope 執行；進行中再呼叫會被忽略
    val status: StateFlow<SyncStatus>   // 狀態，不是事件：任何時候開始收集都拿得到目前值
}
