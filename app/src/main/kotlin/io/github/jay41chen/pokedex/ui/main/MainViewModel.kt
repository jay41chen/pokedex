package io.github.jay41chen.pokedex.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jay41chen.pokedex.domain.model.SyncResult
import io.github.jay41chen.pokedex.domain.model.SyncStatus
import io.github.jay41chen.pokedex.domain.usecase.ObserveSyncStatus
import io.github.jay41chen.pokedex.domain.usecase.RequestSync
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val requestSync: RequestSync,
    private val observeSyncStatus: ObserveSyncStatus,
) : ViewModel() {
    fun onAppForegrounded() = requestSync()

    /**
     * 只有上一輪是 Offline 才重抓：註冊 callback 當下會立即回報一次，無條件觸發會在回前景時多跑一輪，
     * Success 也可能蓋掉還沒顯示的 PartialFailure；觸發後 status 立即變 Running，連續回報不會重複觸發。
     */
    fun onNetworkAvailable() {
        val status = observeSyncStatus().value
        if (status is SyncStatus.Finished && status.result == SyncResult.Offline) requestSync()
    }
}
