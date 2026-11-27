package io.github.jay41chen.pokedex.data.local

import io.github.jay41chen.pokedex.data.local.dao.CaptureDao
import io.github.jay41chen.pokedex.data.local.entity.CaptureEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.mapper.toCard
import io.github.jay41chen.pokedex.domain.model.CaptureId
import io.github.jay41chen.pokedex.domain.model.CapturedPokemon
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.repository.CaptureRepository
import io.github.jay41chen.pokedex.domain.time.TimeSource
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class CaptureRepositoryImpl(
    private val dao: CaptureDao,
    private val timeSource: TimeSource,
) : CaptureRepository {

    override fun observeCaptures(): Flow<List<CapturedPokemon>> =
        dao.observeCaptures().map { rows ->
            rows.map { row ->
                CapturedPokemon(
                    captureId = CaptureId(row.captureId),
                    pokemon = toCard(
                        PokemonEntity(row.pokemonId, row.name),
                        row.imageUrl?.let { PokemonDetailEntity(row.pokemonId, it) },
                    ),
                    capturedAtEpochMillis = row.capturedAt,
                )
            }
        }

    override suspend fun capture(id: PokemonId) {
        dao.upsert(CaptureEntity(UUID.randomUUID().toString(), id.value, timeSource.nowEpochMillis()))
    }

    override suspend fun release(captureId: CaptureId) {
        dao.deleteById(captureId.value)
    }
}
