package io.github.jay41chen.pokedex.data.sync

import io.github.jay41chen.pokedex.data.mapper.InvalidDataException
import io.github.jay41chen.pokedex.data.mapper.toEntities
import io.github.jay41chen.pokedex.data.mapper.toEntity
import io.github.jay41chen.pokedex.data.remote.PokeApi
import io.github.jay41chen.pokedex.data.remote.PokeApiUrls
import io.github.jay41chen.pokedex.data.remote.isRetryableHttpCode
import io.github.jay41chen.pokedex.domain.model.PokemonScope
import io.github.jay41chen.pokedex.domain.model.SyncResult
import io.github.jay41chen.pokedex.domain.model.SyncStatus
import io.github.jay41chen.pokedex.domain.repository.SyncRepository
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

internal class DefaultSyncRepository(
    private val api: PokeApi,
    private val store: SyncStore,
    private val connectivity: ConnectivityChecker,
    private val reporter: PermanentFailureReporter,
    private val scope: CoroutineScope,
) : SyncRepository {

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()

    override fun requestSync() {
        // State machine: Idle/Finished --(requestSync, CAS)--> Running --(round ends, single writer)--> Finished.
        // The CAS makes two simultaneous calls start only one round (spec round 4b).
        // The round number is carried by Finished itself: next = previous Finished.round + 1 (Idle -> 0).
        // It is read from the state the CAS replaced, so no separate counter field is needed.
        var previousRound = 0L
        while (true) {
            val current = _status.value
            if (current == SyncStatus.Running) return
            val previous = (current as? SyncStatus.Finished)?.round ?: 0L
            if (_status.compareAndSet(current, SyncStatus.Running)) {
                previousRound = previous
                break
            }
        }
        // Cancellation propagates through Retrofit suspend functions to the OkHttp call (spec round 4b).
        // The application scope is only cancelled when the process ends; not handled separately.
        scope.launch {
            // The finally block guarantees status is written back to Finished however the block ends
            // (including cancellation and Error), per spec round 4b: status must never stay in Running.
            // If the scope is really cancelled (process ending), the write-back is harmless.
            var result: SyncResult = SyncResult.PartialFailure
            try {
                result = runRound()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Unexpected exceptions (pendingIds, isOnline, reporter, ...) must not leave status stuck
                // in Running. Missing data stays in pendingIds and is fetched on the next foreground.
                // TODO: inject a logger in the Hilt stage
            } finally {
                _status.value = SyncStatus.Finished(previousRound + 1, result)
            }
        }
    }

    private sealed interface Job {
        val id: Int

        data class Detail(override val id: Int) : Job
        data class Species(override val id: Int) : Job
    }

    private enum class Kind { Transient, Permanent, Offline }

    /** Thrown to cancel all workers when connectivity is lost mid-round. */
    private class OfflineAbort : Exception()

    private class RoundState {
        val transientFailure = AtomicBoolean(false)
        val localWriteFailure = AtomicBoolean(false)
    }

    private suspend fun runRound(): SyncResult {
        if (!connectivity.isOnline()) return SyncResult.Offline

        var pending = store.pendingIds()
        if (pending.needList) {
            val limit = PokemonScope.MAX_ID
            val entities = try {
                api.list(limit).toEntities()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return when (classify(e)) {
                    Kind.Offline -> SyncResult.Offline
                    Kind.Transient -> SyncResult.PartialFailure
                    Kind.Permanent -> {
                        safeReport(PokeApiUrls.list(limit), e)
                        // Per spec (PO confirmed): permanent failures don't affect the result, so a list
                        // failure with no data still returns Success. Listed in the backlog.
                        SyncResult.Success
                    }
                }
            }
            try {
                store.writeList(entities)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // TODO: inject a logger in the Hilt stage
                return SyncResult.PartialFailure
            }
            pending = store.pendingIds()
        }

        val queue = Channel<Job>(Channel.UNLIMITED)
        pending.detailIds.forEach { queue.trySend(Job.Detail(it)) }
        pending.speciesIds.forEach { queue.trySend(Job.Species(it)) }
        queue.close()

        val state = RoundState()
        try {
            coroutineScope {
                repeat(WORKER_COUNT) {
                    launch { for (job in queue) process(job, state) }
                }
            }
        } catch (_: OfflineAbort) {
            return SyncResult.Offline
        }
        return if (state.transientFailure.get() || state.localWriteFailure.get()) {
            SyncResult.PartialFailure
        } else {
            SyncResult.Success
        }
    }

    private suspend fun process(job: Job, state: RoundState) {
        when (job) {
            is Job.Detail -> {
                val (detail, types) = fetch(PokeApiUrls.pokemon(job.id), state) {
                    api.pokemon(job.id).toEntities(job.id)
                } ?: return
                write(state) { store.writeDetail(detail, types) }
            }
            is Job.Species -> {
                val species = fetch(PokeApiUrls.species(job.id), state) {
                    api.species(job.id).toEntity(job.id)
                } ?: return
                write(state) { store.writeSpecies(species) }
            }
        }
    }

    /** Fetch + convert. Returns null if this job failed (already classified and recorded). */
    private suspend fun <T> fetch(url: String, state: RoundState, block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            when (classify(e)) {
                Kind.Offline -> throw OfflineAbort()
                Kind.Transient -> state.transientFailure.set(true) // TODO: inject a logger in the Hilt stage
                Kind.Permanent -> safeReport(url, e)
            }
            null
        }

    /**
     * A failed write leaves no row, so the id stays in pendingIds and is re-fetched the next time
     * the app returns to the foreground. No buffer.
     */
    private suspend fun write(state: RoundState, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state.localWriteFailure.set(true) // TODO: inject a logger in the Hilt stage
        }
    }

    /**
     * The reporter only does side work such as logging or clearing caches. Its failure must not abort
     * the round or make the result worse.
     */
    private fun safeReport(url: String, cause: Exception) {
        try {
            reporter.report(url, cause)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // TODO: inject a logger in the Hilt stage and log a line here
        }
    }

    private fun classify(e: Exception): Kind = when (e) {
        is IOException -> if (!connectivity.isOnline()) Kind.Offline else Kind.Transient
        is HttpException -> if (isRetryableHttpCode(e.code())) Kind.Transient else Kind.Permanent
        is SerializationException, is InvalidDataException -> Kind.Permanent
        // [待確認] spec 未列，視為暫時性以免狀態卡住
        else -> Kind.Transient
    }

    private companion object {
        const val WORKER_COUNT = 5
    }
}
