@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package io.github.jay41chen.pokedex.data.sync

import io.github.jay41chen.pokedex.data.logging.DataLogger
import io.github.jay41chen.pokedex.data.remote.PokeApiUrls
import io.github.jay41chen.pokedex.data.remote.dto.PokemonListDto
import io.github.jay41chen.pokedex.domain.model.SyncResult
import io.github.jay41chen.pokedex.domain.model.SyncStatus
import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSyncRepositoryTest {

    private class Env(scope: kotlinx.coroutines.CoroutineScope) {
        val api = FakePokeApi()
        val store = FakeSyncStore()
        val connectivity = FakeConnectivity()
        val reporter = FakeReporter()
        val repo = DefaultSyncRepository(api, store, connectivity, reporter, scope, DataLogger { _, _ -> })
    }

    private val ids = (1..151).toList()

    // backgroundScope tasks are not advanced by advanceUntilIdle() (only foreground tasks count),
    // so the repo runs in the TestScope itself.
    private fun TestScope.env() = Env(this)

    @Test
    fun `S1 mid-round disconnect aborts as Offline and next round resumes from missing ids`() = runTest {
        val e = env()
        e.api.detailHook = { seq, _ ->
            if (seq == 12) {
                e.connectivity.online = false
                throw IOException("lost")
            }
        }
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.Offline), e.repo.status.value)
        assertTrue("detail 1..11 stored, got ${e.store.detail.sorted()}", e.store.detail.containsAll((1..11).toList()))

        e.connectivity.online = true
        e.api.detailHook = null
        e.api.clearRequests()
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(2, SyncResult.Success), e.repo.status.value)
        val requested = e.api.detailRequests
        assertTrue("round 2 must not re-request 1..11: $requested", requested.none { it in 1..11 })
        assertEquals(12, requested.min())
        assertEquals(ids, e.store.detail.sorted())
    }

    @Test
    fun `S2 offline at start makes no requests`() = runTest {
        val e = env()
        e.connectivity.online = false
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.Offline), e.repo.status.value)
        assertEquals(0, e.api.listCalls.get())
        assertEquals(0, e.api.detailRequests.size)
        assertEquals(0, e.api.speciesRequests.size)
    }

    @Test
    fun `S3 persistent 503 is PartialFailure and only that id is retried next round`() = runTest {
        val e = env()
        e.api.detailFailures[7] = { httpException(503) }
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.PartialFailure), e.repo.status.value)

        e.api.clearRequests()
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(2, SyncResult.PartialFailure), e.repo.status.value)
        assertEquals(listOf(7), e.api.detailRequests.toList())
        assertEquals(emptyList<Int>(), e.api.speciesRequests.toList())
    }

    @Test
    fun `S4 404 is permanent - reported, round still Success`() = runTest {
        val e = env()
        e.api.detailFailures[7] = { httpException(404) }
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.Success), e.repo.status.value)
        assertEquals(ids - 7, e.store.detail.sorted())
        assertEquals(ids, e.store.species.sorted())
        assertEquals(listOf(PokeApiUrls.pokemon(7)), e.reporter.urls)
    }

    @Test
    fun `S5 two requestSync calls start one round`() = runTest {
        val e = env()
        e.repo.requestSync()
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(1, e.api.listCalls.get())
        assertEquals(SyncStatus.Finished(1, SyncResult.Success), e.repo.status.value)
    }

    @Test
    fun `S6 status is Running immediately and round increments`() = runTest {
        val e = env()
        assertEquals(SyncStatus.Idle, e.repo.status.value)
        e.repo.requestSync()
        assertEquals(SyncStatus.Running, e.repo.status.value)
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.Success), e.repo.status.value)
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(2, SyncResult.Success), e.repo.status.value)
    }

    @Test
    fun `mid-round disconnect issues no new requests after the trigger`() = runTest {
        val e = env()
        var totalAtTrigger = -1
        e.api.detailHook = { seq, _ ->
            if (seq == 40) {
                e.connectivity.online = false
                totalAtTrigger = e.api.totalRequests
                throw IOException("lost")
            }
        }
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.Offline), e.repo.status.value)
        assertTrue(totalAtTrigger > 0)
        assertEquals(totalAtTrigger, e.api.totalRequests)
        assertTrue(e.api.totalRequests < 303)
    }

    @Test
    fun `five workers run concurrently`() = runTest {
        val e = env()
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.Success), e.repo.status.value)
        assertEquals(5, e.api.maxInFlight)
    }

    @Test
    fun `local write failure is PartialFailure without cancelling other work`() = runTest {
        val e = env()
        e.store.failDetailWrite(7)
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.PartialFailure), e.repo.status.value)
        assertEquals(ids - 7, e.store.detail.sorted())
        assertEquals(ids, e.store.species.sorted())
        assertEquals(emptyList<String>(), e.reporter.urls)
    }

    @Test
    fun `unexpected exception from pendingIds is PartialFailure and next round still runs`() = runTest {
        val e = env()
        e.store.pendingIdsError = RuntimeException("db broke")
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.PartialFailure), e.repo.status.value)

        e.store.pendingIdsError = null
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(2, SyncResult.Success), e.repo.status.value)
    }

    @Test
    fun `CancellationException from pendingIds still ends Finished PartialFailure and next round runs`() = runTest {
        val e = env()
        e.store.pendingIdsError = kotlin.coroutines.cancellation.CancellationException("fake")
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.PartialFailure), e.repo.status.value)

        e.store.pendingIdsError = null
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(2, SyncResult.Success), e.repo.status.value)
    }

    /**
     * An Error propagates out of launch, so the repo gets its own scope with a no-op exception handler
     * instead of the TestScope (an uncaught exception there would fail runTest).
     */
    @Test
    fun `Error from pendingIds still ends Finished PartialFailure`() = runTest {
        val scope = CoroutineScope(
            SupervisorJob() + StandardTestDispatcher(testScheduler) + CoroutineExceptionHandler { _, _ -> },
        )
        try {
            val e = Env(scope)
            e.store.pendingIdsError = Error("fake")
            e.repo.requestSync()
            advanceUntilIdle()
            assertEquals(SyncStatus.Finished(1, SyncResult.PartialFailure), e.repo.status.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `reporter throwing does not abort the round`() = runTest {
        val e = env()
        e.api.detailFailures[7] = { httpException(404) }
        e.reporter.throwOnReport = IllegalStateException("reporter broke")
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.Success), e.repo.status.value)
        assertEquals(ids - 7, e.store.detail.sorted())
        assertEquals(ids, e.store.species.sorted())
    }

    /**
     * All 151 detail requests start before any species request. With 5 workers pulling from one FIFO
     * channel on the single-threaded test dispatcher, a request is recorded right after its job is
     * dequeued (no suspension in between), so start order equals queue order and this is deterministic.
     */
    @Test
    fun `detail requests all start before any species request`() = runTest {
        val e = env()
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.Success), e.repo.status.value)
        val order = e.api.startOrder.toList()
        assertEquals(302, order.size)
        assertTrue("first 151 must be detail: ${order.take(151)}", order.take(151).all { it.startsWith("detail:") })
        assertTrue("last 151 must be species", order.drop(151).all { it.startsWith("species:") })
    }

    @Test
    fun `Offline takes priority over an earlier transient failure`() = runTest {
        val e = env()
        e.api.detailFailures[3] = { httpException(503) }
        e.api.detailHook = { seq, _ ->
            if (seq == 40) {
                e.connectivity.online = false
                throw IOException("lost")
            }
        }
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.Offline), e.repo.status.value)
    }

    @Test
    fun `429 is transient - PartialFailure and not reported`() = runTest {
        val e = env()
        e.api.detailFailures[7] = { httpException(429) }
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.PartialFailure), e.repo.status.value)
        assertEquals(emptyList<String>(), e.reporter.urls)
    }

    @Test
    fun `list 404 is permanent - reported, round Success, no detail or species requests`() = runTest {
        val e = env()
        e.api.listResult = { throw httpException(404) }
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.Success), e.repo.status.value)
        assertEquals(listOf(PokeApiUrls.list(151)), e.reporter.urls)
        assertEquals(0, e.api.detailRequests.size)
        assertEquals(0, e.api.speciesRequests.size)
    }

    @Test
    fun `list 503 is transient - PartialFailure, no detail or species requests`() = runTest {
        val e = env()
        e.api.listResult = { throw httpException(503) }
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.PartialFailure), e.repo.status.value)
        assertEquals(0, e.api.detailRequests.size)
        assertEquals(0, e.api.speciesRequests.size)
    }

    @Test
    fun `list write failure is PartialFailure`() = runTest {
        val e = env()
        e.store.writeListError = RuntimeException("disk full")
        e.repo.requestSync()
        advanceUntilIdle()
        assertEquals(SyncStatus.Finished(1, SyncResult.PartialFailure), e.repo.status.value)
        assertEquals(0, e.api.detailRequests.size)
        assertEquals(0, e.api.speciesRequests.size)
    }

    /**
     * Real threads on Dispatchers.Default (not the test dispatcher) so requestSync() calls really race.
     * list() is gated so the round cannot finish before every caller has tried; list then returns an
     * empty result so each iteration ends fast. Repeated with fresh repos to make a broken CAS likely to show.
     */
    @Test
    fun `concurrent requestSync calls start exactly one round`() {
        repeat(30) { iteration ->
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val api = FakePokeApi()
                val gate = CompletableDeferred<Unit>()
                api.listGate = gate
                api.listResult = { PokemonListDto(emptyList()) }
                val repo = DefaultSyncRepository(api, FakeSyncStore(), FakeConnectivity(), FakeReporter(), scope, DataLogger { _, _ -> })

                val start = CountDownLatch(1)
                val threads = List(8) {
                    thread {
                        start.await()
                        repeat(100) { repo.requestSync() }
                    }
                }
                start.countDown()
                threads.forEach { it.join() }
                // give any (buggy) extra rounds time to reach list()
                Thread.sleep(20)
                gate.complete(Unit)
                runBlocking {
                    withTimeout(10_000) { repo.status.first { it is SyncStatus.Finished } }
                }
                assertEquals("iteration $iteration", 1, api.listCalls.get())
                assertEquals("iteration $iteration", SyncStatus.Finished(1, SyncResult.Success), repo.status.value)
            } finally {
                scope.cancel()
            }
        }
    }
}
