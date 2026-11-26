package io.github.jay41chen.pokedex.domain.repository

import io.github.jay41chen.pokedex.domain.model.CaptureId
import io.github.jay41chen.pokedex.domain.model.CapturedPokemon
import io.github.jay41chen.pokedex.domain.model.PokemonId
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun observeCaptures(): Flow<List<CapturedPokemon>>  // 新的在前：captured_at DESC, rowid DESC
    suspend fun capture(id: PokemonId)                  // 一定成功
    suspend fun release(captureId: CaptureId)           // 冪等：不存在就不做事
}
