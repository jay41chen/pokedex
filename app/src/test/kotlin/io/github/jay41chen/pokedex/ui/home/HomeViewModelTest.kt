package io.github.jay41chen.pokedex.ui.home

import io.github.jay41chen.pokedex.domain.model.CaptureId
import io.github.jay41chen.pokedex.domain.model.CatalogEntry
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.SyncResult
import io.github.jay41chen.pokedex.domain.model.SyncStatus
import io.github.jay41chen.pokedex.domain.model.TypeName
import io.github.jay41chen.pokedex.domain.usecase.CapturePokemon
import io.github.jay41chen.pokedex.domain.usecase.ObserveCaptures
import io.github.jay41chen.pokedex.domain.usecase.ObserveSyncStatus
import io.github.jay41chen.pokedex.domain.usecase.ObserveTypeSections
import io.github.jay41chen.pokedex.domain.usecase.ReleaseCapture
import io.github.jay41chen.pokedex.testing.FakeAppLogger
import io.github.jay41chen.pokedex.testing.FakeCaptureRepository
import io.github.jay41chen.pokedex.testing.FakePokemonRepository
import io.github.jay41chen.pokedex.testing.FakeSyncRepository
import io.github.jay41chen.pokedex.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pokemonRepo = FakePokemonRepository()
    private val captureRepo = FakeCaptureRepository()
    private val syncRepo = FakeSyncRepository()
    private val logger = FakeAppLogger()

    private fun createViewModel() = HomeViewModel(
        observeTypeSections = ObserveTypeSections(pokemonRepo),
        observeCaptures = ObserveCaptures(captureRepo),
        observeSyncStatus = ObserveSyncStatus(syncRepo),
        capturePokemon = CapturePokemon(captureRepo),
        releaseCapture = ReleaseCapture(captureRepo),
        logger = logger,
    )

    private fun bulbasaur() = CatalogEntry(
        card = PokemonCard(PokemonId(1), "Bulbasaur", "https://example.com/1.png"),
        types = listOf(TypeName("grass"), TypeName("poison")),
    )

    private val offlineRound1 = SyncStatus.Finished(1, SyncResult.Offline)

    @Test
    fun `V1 empty catalog with Finished Offline shows Empty offline and Offline message`() {
        val vm = createViewModel()

        syncRepo.status.value = offlineRound1

        assertEquals(CatalogStatus.Empty(offline = true), vm.state.value.catalog)
        assertEquals(UserMessage.Offline, vm.state.value.message)
    }

    @Test
    fun `V2 message stays cleared after MessageShown when catalog emits a new value`() {
        val vm = createViewModel()
        syncRepo.status.value = offlineRound1
        assertEquals(UserMessage.Offline, vm.state.value.message)

        vm.onIntent(HomeIntent.MessageShown)
        pokemonRepo.catalog.value = listOf(bulbasaur())

        assertEquals(null, vm.state.value.message)
        assertEquals(CatalogStatus.Ready, vm.state.value.catalog)
    }

    @Test
    fun `V3 status already Finished Offline before VM creation still yields Offline message`() {
        syncRepo.status.value = offlineRound1

        val vm = createViewModel()

        assertEquals(UserMessage.Offline, vm.state.value.message)
    }

    @Test
    fun `V4 Running does not drop catalog back to Loading after Finished Offline`() {
        val vm = createViewModel()

        syncRepo.status.value = SyncStatus.Running
        assertEquals(CatalogStatus.Loading, vm.state.value.catalog)

        syncRepo.status.value = offlineRound1
        assertEquals(CatalogStatus.Empty(offline = true), vm.state.value.catalog)

        syncRepo.status.value = SyncStatus.Running
        assertEquals(CatalogStatus.Empty(offline = true), vm.state.value.catalog)
    }

    @Test
    fun `V5 Finished Success after unshown Offline clears message and shows Empty online`() {
        val vm = createViewModel()

        syncRepo.status.value = offlineRound1
        assertEquals(UserMessage.Offline, vm.state.value.message)

        syncRepo.status.value = SyncStatus.Finished(2, SyncResult.Success)
        assertEquals(null, vm.state.value.message)
        assertEquals(CatalogStatus.Empty(offline = false), vm.state.value.catalog)
    }

    @Test
    fun `V6 Finished before first catalog emission stays Loading until catalog emits`() {
        val source = MutableSharedFlow<List<CatalogEntry>>(replay = 1)
        pokemonRepo.catalogSource = source
        syncRepo.status.value = SyncStatus.Finished(1, SyncResult.Success)

        val vm = createViewModel()
        assertEquals(CatalogStatus.Loading, vm.state.value.catalog)

        source.tryEmit(listOf(bulbasaur()))
        assertEquals(CatalogStatus.Ready, vm.state.value.catalog)
    }

    @Test
    fun `V6b Finished then first catalog emission empty yields Empty`() {
        val source = MutableSharedFlow<List<CatalogEntry>>(replay = 1)
        pokemonRepo.catalogSource = source
        syncRepo.status.value = SyncStatus.Finished(1, SyncResult.Success)

        val vm = createViewModel()
        source.tryEmit(emptyList())

        assertEquals(CatalogStatus.Empty(offline = false), vm.state.value.catalog)
    }

    @Test
    fun `V10 message is held while catalog is Loading and released when catalog emits`() {
        val source = MutableSharedFlow<List<CatalogEntry>>(replay = 1)
        pokemonRepo.catalogSource = source
        syncRepo.status.value = SyncStatus.Finished(1, SyncResult.Offline)

        val vm = createViewModel()
        assertEquals(CatalogStatus.Loading, vm.state.value.catalog)
        assertEquals(null, vm.state.value.message)

        source.tryEmit(listOf(bulbasaur()))
        assertEquals(CatalogStatus.Ready, vm.state.value.catalog)
        assertEquals(UserMessage.Offline, vm.state.value.message)
    }

    @Test
    fun `V7 Capture write failure is logged and VM keeps working`() {
        val error = IllegalStateException("disk full")
        captureRepo.captureError = error
        val vm = createViewModel()

        vm.onIntent(HomeIntent.Capture(PokemonId(1)))

        assertEquals(1, logger.warnings.size)
        val (message, cause) = logger.warnings.single()
        assertTrue(message, "Capture failed" in message && "id=1" in message)
        assertSame(error, cause)
        // VM 沒掛：仍能處理後續 intent 與同步狀態
        syncRepo.status.value = offlineRound1
        assertEquals(UserMessage.Offline, vm.state.value.message)
        vm.onIntent(HomeIntent.MessageShown)
        assertEquals(null, vm.state.value.message)
    }

    @Test
    fun `V8 Release write failure is logged and VM keeps working`() {
        val error = IllegalStateException("disk full")
        captureRepo.releaseError = error
        val vm = createViewModel()

        vm.onIntent(HomeIntent.Release(CaptureId("c1")))

        assertEquals(1, logger.warnings.size)
        val (message, cause) = logger.warnings.single()
        assertTrue(message, "Release failed" in message && "captureId=c1" in message)
        assertSame(error, cause)
        syncRepo.status.value = offlineRound1
        assertEquals(UserMessage.Offline, vm.state.value.message)
        vm.onIntent(HomeIntent.MessageShown)
        assertEquals(null, vm.state.value.message)
    }
}
