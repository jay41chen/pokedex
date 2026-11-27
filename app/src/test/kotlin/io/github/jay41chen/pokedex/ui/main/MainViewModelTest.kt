package io.github.jay41chen.pokedex.ui.main

import io.github.jay41chen.pokedex.domain.model.SyncResult
import io.github.jay41chen.pokedex.domain.model.SyncStatus
import io.github.jay41chen.pokedex.domain.usecase.ObserveSyncStatus
import io.github.jay41chen.pokedex.domain.usecase.RequestSync
import io.github.jay41chen.pokedex.testing.FakeSyncRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {
    private val syncRepo = FakeSyncRepository()

    private fun createViewModel() = MainViewModel(
        requestSync = RequestSync(syncRepo),
        observeSyncStatus = ObserveSyncStatus(syncRepo),
    )

    @Test
    fun `V9 offline result triggers one sync when network becomes available`() {
        val vm = createViewModel()
        syncRepo.status.value = SyncStatus.Finished(1, SyncResult.Offline)

        vm.onNetworkAvailable()

        assertEquals(1, syncRepo.requestSyncCount)
    }

    @Test
    fun `V9 success result does not trigger sync when network becomes available`() {
        val vm = createViewModel()
        syncRepo.status.value = SyncStatus.Finished(1, SyncResult.Success)

        vm.onNetworkAvailable()

        assertEquals(0, syncRepo.requestSyncCount)
    }

    @Test
    fun `V9 partial failure result does not trigger sync when network becomes available`() {
        val vm = createViewModel()
        syncRepo.status.value = SyncStatus.Finished(1, SyncResult.PartialFailure)

        vm.onNetworkAvailable()

        assertEquals(0, syncRepo.requestSyncCount)
    }
}
