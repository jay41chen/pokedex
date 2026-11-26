package io.github.jay41chen.pokedex.data.sync

import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonSpeciesEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonTypeEntity
import io.github.jay41chen.pokedex.data.remote.PokeApi
import io.github.jay41chen.pokedex.data.remote.dto.PokemonDto
import io.github.jay41chen.pokedex.data.remote.dto.PokemonListDto
import io.github.jay41chen.pokedex.data.remote.dto.PokemonSpeciesDto
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

internal class FakeConnectivity(@Volatile var online: Boolean = true) : ConnectivityChecker {
    override fun isOnline() = online
}

internal class FakeReporter : PermanentFailureReporter {
    val reports = CopyOnWriteArrayList<Pair<String, Throwable>>()

    /** If set, report() records the url and then throws this. */
    @Volatile var throwOnReport: Throwable? = null
    override fun report(url: String, cause: Throwable) {
        reports += url to cause
        throwOnReport?.let { throw it }
    }
    val urls: List<String> get() = reports.map { it.first }
}

/** In-memory SyncStore with the same pendingIds semantics as the Room version. */
internal class FakeSyncStore : SyncStore {
    val pokemon: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    val detail: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    val species: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    private val failingDetailWrites: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    fun failDetailWrite(id: Int) {
        failingDetailWrites += id
    }

    /** If set, pendingIds() throws this. */
    @Volatile var pendingIdsError: Throwable? = null

    /** If set, writeList() throws this. */
    @Volatile var writeListError: Throwable? = null

    override suspend fun pendingIds(): PendingWork {
        pendingIdsError?.let { throw it }
        return pendingWork()
    }

    private fun pendingWork() = PendingWork(
        needList = pokemon.isEmpty(),
        detailIds = pokemon.filter { it !in detail }.sorted(),
        speciesIds = pokemon.filter { it !in species }.sorted(),
    )

    override suspend fun writeList(pokemon: List<PokemonEntity>) {
        writeListError?.let { throw it }
        this.pokemon += pokemon.map { it.id }
    }

    override suspend fun writeDetail(detail: PokemonDetailEntity, types: List<PokemonTypeEntity>) {
        if (detail.pokemonId in failingDetailWrites) throw RuntimeException("disk write failed")
        this.detail += detail.pokemonId
    }

    override suspend fun writeSpecies(species: PokemonSpeciesEntity) {
        this.species += species.pokemonId
    }
}

internal fun httpException(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

internal class FakePokeApi : PokeApi {
    val listCalls = AtomicInteger(0)
    val detailRequests: MutableList<Int> = CopyOnWriteArrayList()
    val speciesRequests: MutableList<Int> = CopyOnWriteArrayList()

    /** Requests in the order they started, e.g. "detail:7", "species:7". */
    val startOrder: MutableList<String> = CopyOnWriteArrayList()
    private val inFlight = AtomicInteger(0)
    private val maxInFlightRef = AtomicInteger(0)
    val maxInFlight: Int get() = maxInFlightRef.get()
    val totalRequests: Int get() = detailRequests.size + speciesRequests.size

    /** If set, list() suspends until this completes. */
    @Volatile var listGate: CompletableDeferred<Unit>? = null
    @Volatile var listResult: () -> PokemonListDto = {
        PokemonListDto((1..151).map { PokemonListDto.Item("p$it", "https://pokeapi.co/api/v2/pokemon/$it/") })
    }
    val detailFailures = ConcurrentHashMap<Int, () -> Throwable>()
    val speciesFailures = ConcurrentHashMap<Int, () -> Throwable>()

    /** Called after the 1ms delay of each detail request with (1-based sequence number, id); may throw. */
    @Volatile var detailHook: ((Int, Int) -> Unit)? = null
    private val detailSeq = AtomicInteger(0)

    fun clearRequests() {
        detailRequests.clear()
        speciesRequests.clear()
        startOrder.clear()
        detailSeq.set(0)
    }

    private suspend fun <T> tracked(body: suspend () -> T): T {
        val now = inFlight.incrementAndGet()
        maxInFlightRef.updateAndGet { maxOf(it, now) }
        try {
            delay(1)
            return body()
        } finally {
            inFlight.decrementAndGet()
        }
    }

    override suspend fun list(limit: Int): PokemonListDto {
        listCalls.incrementAndGet()
        listGate?.await()
        return listResult()
    }

    override suspend fun pokemon(id: Int): PokemonDto {
        detailRequests += id
        startOrder += "detail:$id"
        val seq = detailSeq.incrementAndGet()
        return tracked {
            detailHook?.invoke(seq, id)
            detailFailures[id]?.let { throw it() }
            PokemonDto(types = listOf(PokemonDto.TypeSlot(1, PokemonDto.TypeRef("grass"))))
        }
    }

    override suspend fun species(id: Int): PokemonSpeciesDto {
        speciesRequests += id
        startOrder += "species:$id"
        return tracked {
            speciesFailures[id]?.let { throw it() }
            PokemonSpeciesDto(evolvesFromSpecies = null, flavorTextEntries = emptyList())
        }
    }
}
