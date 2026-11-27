package io.github.jay41chen.pokedex.data.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jay41chen.pokedex.data.BuildConfig
import io.github.jay41chen.pokedex.data.local.CaptureRepositoryImpl
import io.github.jay41chen.pokedex.data.local.PokedexDatabase
import io.github.jay41chen.pokedex.data.local.PokemonRepositoryImpl
import io.github.jay41chen.pokedex.data.local.RoomSyncStore
import io.github.jay41chen.pokedex.data.local.dao.CaptureDao
import io.github.jay41chen.pokedex.data.local.dao.PokemonDao
import io.github.jay41chen.pokedex.data.local.dao.SyncDao
import io.github.jay41chen.pokedex.data.logging.AndroidDataLogger
import io.github.jay41chen.pokedex.data.logging.DataLogger
import io.github.jay41chen.pokedex.data.remote.AndroidConnectivityChecker
import io.github.jay41chen.pokedex.data.remote.CacheEvictingReporter
import io.github.jay41chen.pokedex.data.remote.PokeApi
import io.github.jay41chen.pokedex.data.remote.PokeApiJson
import io.github.jay41chen.pokedex.data.remote.PokeApiUrls
import io.github.jay41chen.pokedex.data.remote.RetryInterceptor
import io.github.jay41chen.pokedex.data.remote.ThreadSleeper
import io.github.jay41chen.pokedex.data.sync.ConnectivityChecker
import io.github.jay41chen.pokedex.data.sync.DefaultSyncRepository
import io.github.jay41chen.pokedex.data.sync.PermanentFailureReporter
import io.github.jay41chen.pokedex.data.sync.SyncStore
import io.github.jay41chen.pokedex.domain.repository.CaptureRepository
import io.github.jay41chen.pokedex.domain.repository.PokemonRepository
import io.github.jay41chen.pokedex.domain.repository.SyncRepository
import io.github.jay41chen.pokedex.domain.time.TimeSource
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

// [待確認] Bounds a call including all retries; the value is a guess.
internal const val CALL_TIMEOUT_SECONDS = 60L

@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun dataLogger(): DataLogger = AndroidDataLogger

    // [待確認] The cache size (20 MB) is a guess.
    @Provides
    @Singleton
    fun httpCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "http_cache"), 20L * 1024 * 1024)

    @Provides
    @Singleton
    fun okHttpClient(cache: Cache, @ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(RetryInterceptor(ThreadSleeper))
            // After RetryInterceptor so that every attempt is logged.
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .apply { faultInjectionInterceptors(context).forEach { addNetworkInterceptor(it) } }
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun retrofit(okHttp: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(PokeApiUrls.BASE_URL)
            .client(okHttp)
            .addConverterFactory(PokeApiJson.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun pokeApi(retrofit: Retrofit): PokeApi = retrofit.create()

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PokedexDatabase =
        Room.databaseBuilder(context, PokedexDatabase::class.java, "pokedex.db").build()

    @Provides
    @Singleton
    fun syncDao(db: PokedexDatabase): SyncDao = db.syncDao()

    @Provides
    @Singleton
    fun captureDao(db: PokedexDatabase): CaptureDao = db.captureDao()

    @Provides
    @Singleton
    fun pokemonDao(db: PokedexDatabase): PokemonDao = db.pokemonDao()

    @Provides
    @Singleton
    fun syncStore(db: PokedexDatabase): SyncStore = RoomSyncStore(db)

    @Provides
    @Singleton
    fun connectivityChecker(@ApplicationContext context: Context): ConnectivityChecker =
        AndroidConnectivityChecker(context.getSystemService(ConnectivityManager::class.java))

    @Provides
    @Singleton
    fun permanentFailureReporter(cache: Cache, logger: DataLogger): PermanentFailureReporter =
        CacheEvictingReporter(cache, logger)

    @Provides
    @Singleton
    fun timeSource(): TimeSource = TimeSource { System.currentTimeMillis() }

    @Provides
    @Singleton
    fun syncRepository(
        api: PokeApi,
        store: SyncStore,
        connectivity: ConnectivityChecker,
        reporter: PermanentFailureReporter,
        @ApplicationScope scope: CoroutineScope,
        logger: DataLogger,
    ): SyncRepository = DefaultSyncRepository(api, store, connectivity, reporter, scope, logger)

    @Provides
    @Singleton
    fun pokemonRepository(dao: PokemonDao): PokemonRepository = PokemonRepositoryImpl(dao)

    @Provides
    @Singleton
    fun captureRepository(dao: CaptureDao, timeSource: TimeSource): CaptureRepository =
        CaptureRepositoryImpl(dao, timeSource)
}
