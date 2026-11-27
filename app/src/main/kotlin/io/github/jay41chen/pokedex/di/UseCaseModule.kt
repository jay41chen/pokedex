package io.github.jay41chen.pokedex.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jay41chen.pokedex.domain.repository.CaptureRepository
import io.github.jay41chen.pokedex.domain.repository.PokemonRepository
import io.github.jay41chen.pokedex.domain.repository.SyncRepository
import io.github.jay41chen.pokedex.domain.usecase.CapturePokemon
import io.github.jay41chen.pokedex.domain.usecase.ObserveCaptures
import io.github.jay41chen.pokedex.domain.usecase.ObservePokemonDetail
import io.github.jay41chen.pokedex.domain.usecase.ObserveSyncStatus
import io.github.jay41chen.pokedex.domain.usecase.ObserveTypeSections
import io.github.jay41chen.pokedex.domain.usecase.ReleaseCapture
import io.github.jay41chen.pokedex.domain.usecase.RequestSync

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides fun capturePokemon(repo: CaptureRepository) = CapturePokemon(repo)
    @Provides fun releaseCapture(repo: CaptureRepository) = ReleaseCapture(repo)
    @Provides fun observeCaptures(repo: CaptureRepository) = ObserveCaptures(repo)
    @Provides fun observePokemonDetail(repo: PokemonRepository) = ObservePokemonDetail(repo)
    @Provides fun observeTypeSections(repo: PokemonRepository) = ObserveTypeSections(repo)
    @Provides fun observeSyncStatus(repo: SyncRepository) = ObserveSyncStatus(repo)
    @Provides fun requestSync(repo: SyncRepository) = RequestSync(repo)
}
