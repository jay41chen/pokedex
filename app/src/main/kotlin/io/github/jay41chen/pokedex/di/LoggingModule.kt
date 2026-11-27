package io.github.jay41chen.pokedex.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jay41chen.pokedex.logging.AndroidAppLogger
import io.github.jay41chen.pokedex.logging.AppLogger

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {
    @Provides fun appLogger(): AppLogger = AndroidAppLogger
}
